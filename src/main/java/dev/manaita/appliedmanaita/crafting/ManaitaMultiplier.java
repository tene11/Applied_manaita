package dev.manaita.appliedmanaita.crafting;

import java.lang.reflect.Field;

import org.jetbrains.annotations.Nullable;

import net.neoforged.fml.ModList;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;

import dev.manaita.appliedmanaita.AppliedManaita;

/**
 * まな板mod の倍率を読み取る。対応しているまな板mod は次の2つで、両方入っているときは ManaitaPlusGeneral を使う。
 * <ul>
 *     <li>ManaitaPlusGeneral: 設定ファイル {@code config/manaita_plus_general-common.toml}</li>
 *     <li>ManaitaPlus Legacy: 設定ファイル {@code config/manaita_plus_legacy-common.toml}</li>
 * </ul>
 * どちらも設定の名前は同じ:
 * <ul>
 *     <li>{@code crafting_doubling_value}: 作業まな板の倍率 → まな板クラフトパターン(まな板分子組立機)</li>
 *     <li>{@code furnace_doubling_value}: 調理まな板の倍率 → まな板加工パターンのかまどレシピ(まな板分子精錬機)</li>
 *     <li>{@code brewing_doubling_value}: 醸造まな板の倍率 → まな板加工パターンの醸造レシピ(まな板分子醸造機)</li>
 * </ul>
 * ManaitaPlusGeneral のクラスには触らず、NeoForge が読み込んだ設定の値だけを見るので、まな板mod側の実装が変わっても壊れにくい。
 * (設定の値が見つからないときだけ、念のため ManaitaPlusGeneral の設定クラスの値を見る)
 * 設定ファイルを書き換えると NeoForge が読み直すので、ここでも1秒ごとに読み直す。
 * <p>
 * サーバーではサーバーの設定の値を使う。クライアント(画面・説明欄の表示)では、サーバーから送られた値があればそれを使う
 * ({@code network/MultipliersPayload})。マルチプレイで手元の設定がサーバーと違っても、表示がずれないようにするため。
 */
public final class ManaitaMultiplier {

    /** ManaitaPlusGeneral の mod ID */
    public static final String GENERAL_MOD_ID = "manaita_plus_general";
    /** ManaitaPlus Legacy の mod ID */
    public static final String LEGACY_MOD_ID = "manaita_plus_legacy";

    /** 倍率の種類 */
    public enum Kind {
        /** 作業まな板(クラフト) */
        CRAFTING("crafting_doubling_value", "tooltip.appliedmanaita.multiplier"),
        /** 調理まな板(かまど) */
        FURNACE("furnace_doubling_value", "tooltip.appliedmanaita.furnace_multiplier"),
        /** 醸造まな板(醸造台) */
        BREWING("brewing_doubling_value", "tooltip.appliedmanaita.brewing_multiplier");

        private final String configKey;
        private final String tooltipKey;

        Kind(final String configKey, final String tooltipKey) {
            this.configKey = configKey;
            this.tooltipKey = tooltipKey;
        }

        /** 説明欄に出す倍率の行の翻訳キー(引数は倍率) */
        public String tooltipKey() {
            return this.tooltipKey;
        }
    }

    /** ManaitaPlusGeneral の設定の値が入っているクラス(3.0 以降 / それより前) */
    private static final String[] GENERAL_CONFIG_CLASSES = {
            "github.com.gengyoubo.common.config.MPGConfigValues",
            "github.com.gengyoubo.MPG.MPGConfig",
    };
    /** ManaitaPlus Legacy の設定の値が入っているクラス */
    private static final String[] LEGACY_CONFIG_CLASSES = {
            "sen.manaita_plus_legacy.common.config.ManaitaPlusLegacyConfig",
    };

    /** 値が読めないときの倍率。ManaitaPlusGeneral 自身の初期値と同じにする */
    private static final int DEFAULT = 64;

    /** 読み直す間隔(ミリ秒)。CPUの計算中に何度も呼ばれるので、毎回は読まない */
    private static final long CHECK_INTERVAL_MS = 1000;

    private static long lastChecked = Long.MIN_VALUE;
    private static final int[] values = {DEFAULT, DEFAULT, DEFAULT};
    private static boolean warned = false;

    /** サーバーから送られた値(クライアント側の表示用)。届いていなければ null */
    @Nullable
    private static int[] fromServer = null;

    private ManaitaMultiplier() {
    }

    /**
     * 今の倍率。
     *
     * @param clientSide クライアント側(表示用)で使うなら true。サーバーから送られた値があればそちらを使う
     */
    public static synchronized int get(final Kind kind, final boolean clientSide) {
        if (clientSide && fromServer != null) {
            return fromServer[kind.ordinal()];
        }
        reloadIfNeeded();
        return values[kind.ordinal()];
    }

    /** サーバーで使っている値(クライアントへ送る用) */
    public static synchronized int[] getServerValues() {
        reloadIfNeeded();
        return values.clone();
    }

    /** クライアント側: サーバーから送られた値を受け取る */
    public static synchronized void setFromServer(final int crafting, final int furnace, final int brewing) {
        fromServer = new int[]{Math.max(1, crafting), Math.max(1, furnace), Math.max(1, brewing)};
    }

    /** クライアント側: サーバーから抜けたら、送られた値を捨てる */
    public static synchronized void clearFromServer() {
        fromServer = null;
    }

    private static void reloadIfNeeded() {
        final long now = System.currentTimeMillis();
        if (lastChecked != Long.MIN_VALUE && now - lastChecked < CHECK_INTERVAL_MS) {
            return;
        }
        lastChecked = now;
        for (final Kind kind : Kind.values()) {
            values[kind.ordinal()] = Math.max(1, read(kind));
        }
    }

    /** 倍率を読むまな板mod の mod ID(両方入っていれば ManaitaPlusGeneral)。どちらもなければ null */
    @Nullable
    public static String getActiveModId() {
        final ModList mods = ModList.get();
        if (mods == null) {
            return null;
        }
        if (mods.isLoaded(GENERAL_MOD_ID)) {
            return GENERAL_MOD_ID;
        }
        if (mods.isLoaded(LEGACY_MOD_ID)) {
            return LEGACY_MOD_ID;
        }
        return null;
    }

    private static int read(final Kind kind) {
        final String modId = getActiveModId();
        if (modId == null) {
            return DEFAULT;
        }
        // 1. NeoForge が読み込んだまな板mod の設定から
        try {
            for (final ModConfig config : ModConfigs.getModConfigs(modId)) {
                final IConfigSpec.ILoadedConfig loaded = config.getLoadedConfig();
                if (loaded == null) {
                    continue;
                }
                final Object value = loaded.config().get(kind.configKey);
                if (value instanceof Number number) {
                    return clamp(number.longValue());
                }
            }
        } catch (final RuntimeException e) {
            warnOnce(modId + " の設定を読めませんでした", e);
        }

        // 2. 念のため、まな板mod の設定クラスの値から
        Throwable failure = null;
        for (final String className : GENERAL_MOD_ID.equals(modId) ? GENERAL_CONFIG_CLASSES : LEGACY_CONFIG_CLASSES) {
            try {
                final Class<?> configClass = Class.forName(className);
                final Field field = configClass.getField(kind.configKey);
                final Object value = field.get(null);
                if (value instanceof Number number) {
                    return clamp(number.longValue());
                }
            } catch (final ReflectiveOperationException | RuntimeException | LinkageError e) {
                failure = e;
            }
        }
        warnOnce(modId + " の倍率が見つかりませんでした。初期値の " + DEFAULT + " 倍を使います", failure);
        return DEFAULT;
    }

    private static int clamp(final long value) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, value));
    }

    private static void warnOnce(final String message, final Throwable e) {
        if (!warned) {
            warned = true;
            AppliedManaita.LOGGER.warn(message, e);
        }
    }
}

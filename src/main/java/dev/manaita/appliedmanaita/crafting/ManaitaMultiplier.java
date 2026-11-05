package dev.manaita.appliedmanaita.crafting;

import dev.manaita.appliedmanaita.AppliedManaita;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;

/**
 * まな板+の倍率(config/manaita_plus.cfg の Doubling カテゴリ)を読み取る。
 * <ul>
 *     <li>craftingDoubling: 作業まな板の倍化 → まな板パターン(クラフト用、まな板分子組立機)</li>
 *     <li>furnaceDoubling: 調理まな板の倍化 → まな板パターン(かまど用、まな板分子精錬機)</li>
 *     <li>brewingDoubling: 醸造まな板の倍化 → まな板パターン(醸造用、まな板分子醸造機)</li>
 * </ul>
 * まな板+の内部コードには触らず、設定ファイルだけを見るので、まな板+側の実装が変わっても壊れにくい。
 * ファイルの更新日時を見て、変わっていたら読み直す。
 * <p>
 * サーバーではファイルの値を使う。クライアント(画面・説明欄の表示)では、サーバーから送られた値があればそれを使う
 * ({@code network/PacketMultipliers})。マルチプレイで手元の設定がサーバーと違っても、表示がずれないようにするため。
 */
public final class ManaitaMultiplier {

    private static final String CONFIG_FILE = "manaita_plus.cfg";
    private static final String CATEGORY = "Doubling";
    private static final String KEY_CRAFTING = "craftingDoubling";
    private static final String KEY_FURNACE = "furnaceDoubling";
    private static final String KEY_BREWING = "brewingDoubling";

    /** ファイルや値がないときの倍率。まな板+ 自身の初期値と同じにする */
    private static final int DEFAULT_CRAFTING = 2;
    private static final int DEFAULT_FURNACE = 2;
    private static final int DEFAULT_BREWING = 64;

    /** ファイルの更新日時を確かめる間隔(ミリ秒)。CPUの計算中に何度も呼ばれるので、毎回は見に行かない */
    private static final long CHECK_INTERVAL_MS = 1000;

    private static long lastModified = Long.MIN_VALUE;
    private static long lastChecked = Long.MIN_VALUE;
    private static int crafting = DEFAULT_CRAFTING;
    private static int furnace = DEFAULT_FURNACE;
    private static int brewing = DEFAULT_BREWING;

    /** サーバーから送られた値(クライアント側の表示用)。届いていなければ null */
    private static int[] fromServer = null;

    private ManaitaMultiplier() {
    }

    /** 作業まな板の倍率 */
    public static synchronized int getCraftingMultiplier() {
        if (useServerValues()) {
            return fromServer[0];
        }
        reloadIfNeeded();
        return crafting;
    }

    /** 調理まな板(かまど)の倍率 */
    public static synchronized int getFurnaceMultiplier() {
        if (useServerValues()) {
            return fromServer[1];
        }
        reloadIfNeeded();
        return furnace;
    }

    /** 醸造まな板の倍率 */
    public static synchronized int getBrewingMultiplier() {
        if (useServerValues()) {
            return fromServer[2];
        }
        reloadIfNeeded();
        return brewing;
    }

    /** サーバーで使っている値(クライアントへ送る用)。ファイルの値そのもの */
    public static synchronized int[] getServerValues() {
        reloadIfNeeded();
        return new int[]{crafting, furnace, brewing};
    }

    /** クライアント側: サーバーから送られた値を受け取る */
    public static synchronized void setFromServer(final int crafting, final int furnace, final int brewing) {
        fromServer = new int[]{Math.max(1, crafting), Math.max(1, furnace), Math.max(1, brewing)};
    }

    /** クライアント側: サーバーから抜けたら、送られた値を捨てる */
    public static synchronized void clearFromServer() {
        fromServer = null;
    }

    /** クライアントのスレッドから呼ばれていて、サーバーの値が届いているか */
    private static boolean useServerValues() {
        return fromServer != null && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
    }

    private static void reloadIfNeeded() {
        final long now = System.currentTimeMillis();
        if (lastChecked != Long.MIN_VALUE && now - lastChecked < CHECK_INTERVAL_MS) {
            return;
        }
        lastChecked = now;

        final File file = new File(Loader.instance().getConfigDir(), CONFIG_FILE);
        if (!file.exists()) {
            crafting = DEFAULT_CRAFTING;
            furnace = DEFAULT_FURNACE;
            brewing = DEFAULT_BREWING;
            lastModified = Long.MIN_VALUE;
            return;
        }

        final long modified = file.lastModified();
        if (modified != lastModified) {
            lastModified = modified;
            try {
                final Configuration cfg = new Configuration(file);
                // get() は値が無ければデフォルトを返すだけで、ファイルには書き込まない(save()しないため)
                crafting = Math.max(1, cfg.get(CATEGORY, KEY_CRAFTING, DEFAULT_CRAFTING).getInt(DEFAULT_CRAFTING));
                furnace = Math.max(1, cfg.get(CATEGORY, KEY_FURNACE, DEFAULT_FURNACE).getInt(DEFAULT_FURNACE));
                brewing = Math.max(1, cfg.get(CATEGORY, KEY_BREWING, DEFAULT_BREWING).getInt(DEFAULT_BREWING));
            } catch (RuntimeException e) {
                AppliedManaita.LOGGER.warn("まな板+の設定ファイルを読めませんでした。まな板+の初期値の倍率を使います", e);
                crafting = DEFAULT_CRAFTING;
                furnace = DEFAULT_FURNACE;
                brewing = DEFAULT_BREWING;
            }
        }
    }
}

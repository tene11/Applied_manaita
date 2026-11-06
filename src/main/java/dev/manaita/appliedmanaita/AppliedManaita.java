package dev.manaita.appliedmanaita;

import com.mojang.logging.LogUtils;
import dev.manaita.appliedmanaita.network.ModNetwork;
import dev.manaita.appliedmanaita.network.MultiplierSync;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;

/**
 * Applied Manaita : まな板mod (ManaitaPlusGeneral / ManaitaPlus Legacy) と AE2 を連動させるアドオン (1.21.1 NeoForge 版)
 */
@Mod(AppliedManaita.MOD_ID)
public final class AppliedManaita {

    public static final String MOD_ID = "appliedmanaita";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Iris (シェーダーmod) の mod ID。ManaitaPlus Legacy を使うときに必要 */
    private static final String IRIS_MOD_ID = "iris";

    public AppliedManaita(final IEventBus modBus, final ModContainer container) {
        checkManaitaMods();

        ModRegistry.register(modBus);
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(this::commonSetup);

        // 倍率をサーバーからクライアントへ知らせる(ログイン時・設定が変わったとき)
        NeoForge.EVENT_BUS.register(MultiplierSync.class);
    }

    /**
     * まな板mod が入っているか確かめる。
     * <ul>
     *     <li>どちらも入っていない: 分かりやすいメッセージで起動を止める</li>
     *     <li>ManaitaPlus Legacy を使う(ManaitaPlusGeneral がない)のに Iris がない: 起動時に警告を出す
     *     (ManaitaPlus Legacy は Iris が必要とされているため)</li>
     * </ul>
     */
    private static void checkManaitaMods() {
        final String active = ManaitaMultiplier.getActiveModId();
        if (active == null) {
            throw new ModLoadingException(ModLoadingIssue.error("appliedmanaita.loading.no_manaita_mod"));
        }
        LOGGER.info("[Applied Manaita] {} の倍率を使います", active);
        if (ManaitaMultiplier.LEGACY_MOD_ID.equals(active) && !ModList.get().isLoaded(IRIS_MOD_ID)) {
            LOGGER.warn("[Applied Manaita] ManaitaPlus Legacy には Iris が必要とされていますが、Iris が入っていません"
                    + " / ManaitaPlus Legacy is said to need Iris, but Iris is not installed");
            ModLoader.addLoadingIssue(ModLoadingIssue.warning("appliedmanaita.loading.legacy_needs_iris"));
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Mixin が効いていなければ、ここで分かりやすいメッセージを出して止める
            MixinCheck.verify();
            // 加速カードを AE2 の分子組立機と同じく5枚まで入れられるようにする(カードの説明欄にも出る)
            Upgrades.add(AEItems.SPEED_CARD, ModRegistry.MANAITA_ASSEMBLER.get(), 5);
        });
    }

    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}

package dev.manaita.appliedmanaita;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;

import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import dev.manaita.appliedmanaita.network.ModNetwork;
import dev.manaita.appliedmanaita.network.MultiplierSync;

/**
 * Applied Manaita : まな板mod (ManaitaPlusGeneral / ManaitaPlus Legacy / ManaitaMTK) と AE2 を連動させるアドオン (1.20.1 Forge 版)
 */
@Mod(AppliedManaita.MOD_ID)
public final class AppliedManaita {

    public static final String MOD_ID = "appliedmanaita";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AppliedManaita() {
        checkManaitaMods();

        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);
        modBus.addListener(this::commonSetup);
        ModNetwork.register();

        // 倍率をサーバーからクライアントへ知らせる(ログイン時・設定が変わったとき)
        MinecraftForge.EVENT_BUS.register(MultiplierSync.class);
    }

    /**
     * まな板mod が入っているか確かめる。どれも入っていなければ、分かりやすいメッセージで起動を止める
     * (Forge の「読み込みに失敗しました」の画面に、このメッセージが出る)。
     */
    private static void checkManaitaMods() {
        final String active = ManaitaMultiplier.getActiveModId();
        if (active == null) {
            throw new IllegalStateException("まな板mod が必要です。ManaitaPlusGeneral、ManaitaPlus Legacy、ManaitaMTK のどれかを入れてください。"
                    + " / A Manaita mod is required. Please install ManaitaPlusGeneral, ManaitaPlus Legacy or ManaitaMTK.");
        }
        LOGGER.info("[Applied Manaita] {} の倍率を使います", active);
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
        return new ResourceLocation(MOD_ID, path);
    }
}

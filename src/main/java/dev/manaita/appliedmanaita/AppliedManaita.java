package dev.manaita.appliedmanaita;

import com.mojang.logging.LogUtils;
import dev.manaita.appliedmanaita.network.ModNetwork;
import dev.manaita.appliedmanaita.network.MultiplierSync;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;

/**
 * Applied Manaita : ManaitaPlusGeneral と AE2 を連動させるアドオン (1.21.1 NeoForge 版)
 */
@Mod(AppliedManaita.MOD_ID)
public final class AppliedManaita {

    public static final String MOD_ID = "appliedmanaita";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AppliedManaita(final IEventBus modBus, final ModContainer container) {
        ModRegistry.register(modBus);
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(this::commonSetup);

        // 倍率をサーバーからクライアントへ知らせる(ログイン時・設定が変わったとき)
        NeoForge.EVENT_BUS.register(MultiplierSync.class);
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

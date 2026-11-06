package dev.manaita.appliedmanaita.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import appeng.init.client.InitScreens;

import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.ModRegistry;
import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import dev.manaita.appliedmanaita.menu.ManaitaAssemblerMenu;

/**
 * クライアント側だけで行う登録。
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    @Mod.EventBusSubscriber(modid = AppliedManaita.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {

        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            // 画面の登録(AE2 の画面の仕組みを使う。配置は assets/ae2/screens/appliedmanaita/ にある)
            event.enqueueWork(() -> InitScreens.register(ManaitaAssemblerMenu.TYPE, ManaitaAssemblerScreen::new,
                    ManaitaAssemblerScreen.STYLE));
        }

        /**
         * まな板パターンターミナルのアイテムの画面部分に、AE2 の標準色(透明ケーブルの色)を付ける。
         * アイテムの見た目は AE2 のパターンエンコーディングターミナルのモデルを使うので、色の付け方も AE2 と同じにする。
         * (ケーブルに付けたときの色は AE2 がケーブルの色に合わせて付ける)
         */
        @SubscribeEvent
        public static void registerItemColors(final RegisterColorHandlersEvent.Item event) {
            event.register(new StaticItemColor(AEColor.TRANSPARENT), ModRegistry.MANAITA_PATTERN_TERMINAL.get());
        }
    }

    @Mod.EventBusSubscriber(modid = AppliedManaita.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class GameBusEvents {

        private GameBusEvents() {
        }

        /** サーバーから抜けたら、サーバーから送られた倍率を捨てる */
        @SubscribeEvent
        public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
            ManaitaMultiplier.clearFromServer();
        }
    }
}

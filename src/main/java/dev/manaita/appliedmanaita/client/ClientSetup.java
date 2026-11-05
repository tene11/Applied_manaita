package dev.manaita.appliedmanaita.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

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

    @EventBusSubscriber(modid = AppliedManaita.MOD_ID, value = Dist.CLIENT)
    public static final class ModBusEvents {

        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerScreens(final RegisterMenuScreensEvent event) {
            InitScreens.register(event, ManaitaAssemblerMenu.TYPE, ManaitaAssemblerScreen::new, ManaitaAssemblerScreen.STYLE);
        }

        /**
         * まな板パターンターミナルのアイテムの画面部分に、AE2 の標準色(透明ケーブルの色)を付ける。
         * アイテムの見た目は AE2 のパターンエンコーディングターミナルのモデルを使うので、色の付け方も AE2 と同じにする。
         * (ケーブルに付けたときの色は AE2 がケーブルの色に合わせて付ける)
         */
        @SubscribeEvent
        public static void registerItemColors(final RegisterColorHandlersEvent.Item event) {
            final StaticItemColor color = new StaticItemColor(AEColor.TRANSPARENT);
            event.register((stack, tintIndex) -> 0xFF000000 | color.getColor(stack, tintIndex),
                    ModRegistry.MANAITA_PATTERN_TERMINAL.get());
        }
    }

    @EventBusSubscriber(modid = AppliedManaita.MOD_ID, value = Dist.CLIENT)
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

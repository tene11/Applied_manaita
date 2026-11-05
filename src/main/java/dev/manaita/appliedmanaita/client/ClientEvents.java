package dev.manaita.appliedmanaita.client;

import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * クライアント側のイベント。
 */
@Mod.EventBusSubscriber(modid = AppliedManaita.MOD_ID, value = Side.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    /** サーバーから抜けたら、サーバーから送られた倍率を捨てる(次は手元の設定、または次のサーバーの値を使う) */
    @SubscribeEvent
    public static void onDisconnect(final FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ManaitaMultiplier.clearFromServer();
    }
}

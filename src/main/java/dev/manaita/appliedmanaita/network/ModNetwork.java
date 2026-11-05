package dev.manaita.appliedmanaita.network;

import dev.manaita.appliedmanaita.AppliedManaita;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * このMODの通信(サーバー → クライアント)。
 */
public final class ModNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(AppliedManaita.MOD_ID);

    private ModNetwork() {
    }

    /** preInit で呼ぶ */
    public static void register() {
        CHANNEL.registerMessage(PacketMultipliers.Handler.class, PacketMultipliers.class, 0, Side.CLIENT);
    }
}

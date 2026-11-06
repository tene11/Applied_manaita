package dev.manaita.appliedmanaita.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import dev.manaita.appliedmanaita.AppliedManaita;

/**
 * 通信の登録。
 */
public final class ModNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            AppliedManaita.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(MultipliersPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MultipliersPacket::encode)
                .decoder(MultipliersPacket::decode)
                .consumerMainThread(MultipliersPacket::handle)
                .add();
    }
}

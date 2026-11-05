package dev.manaita.appliedmanaita.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;

/**
 * 通信の登録。
 */
public final class ModNetwork {

    private static final String VERSION = "1";

    private ModNetwork() {
    }

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(MultipliersPayload.TYPE, MultipliersPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ManaitaMultiplier.setFromServer(payload.crafting(), payload.furnace(), payload.brewing())));
    }
}

package dev.manaita.appliedmanaita.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.manaita.appliedmanaita.AppliedManaita;

/**
 * サーバー → クライアント: サーバーで使っている倍率(作業まな板・調理まな板・醸造まな板)。
 * クライアントはこれを説明欄などの表示に使う。
 */
public record MultipliersPayload(int crafting, int furnace, int brewing) implements CustomPacketPayload {

    public static final Type<MultipliersPayload> TYPE = new Type<>(AppliedManaita.id("multipliers"));

    public static final StreamCodec<ByteBuf, MultipliersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MultipliersPayload::crafting,
            ByteBufCodecs.VAR_INT, MultipliersPayload::furnace,
            ByteBufCodecs.VAR_INT, MultipliersPayload::brewing,
            MultipliersPayload::new);

    public static MultipliersPayload of(final int[] values) {
        return new MultipliersPayload(values[0], values[1], values[2]);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

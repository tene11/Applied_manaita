package dev.manaita.appliedmanaita.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;

/**
 * サーバー → クライアント: サーバーで使っている倍率(作業まな板・調理まな板・醸造まな板)。
 * クライアントはこれを説明欄などの表示に使う。
 */
public record MultipliersPacket(int crafting, int furnace, int brewing) {

    public static MultipliersPacket of(final int[] values) {
        return new MultipliersPacket(values[0], values[1], values[2]);
    }

    public void encode(final FriendlyByteBuf buf) {
        buf.writeVarInt(this.crafting);
        buf.writeVarInt(this.furnace);
        buf.writeVarInt(this.brewing);
    }

    public static MultipliersPacket decode(final FriendlyByteBuf buf) {
        return new MultipliersPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    /** クライアントのメインスレッドで呼ばれる */
    public void handle(final Supplier<NetworkEvent.Context> context) {
        ManaitaMultiplier.setFromServer(this.crafting, this.furnace, this.brewing);
    }
}

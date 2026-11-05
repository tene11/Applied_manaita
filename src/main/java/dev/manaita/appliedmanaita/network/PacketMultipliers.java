package dev.manaita.appliedmanaita.network;

import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバーで使っている まな板+ の倍率を、クライアントへ知らせる。
 * クライアントはこの値で説明欄・画面の「×64」などを表示する(実際のクラフトは常にサーバーの値で行われる)。
 */
public class PacketMultipliers implements IMessage {

    private int crafting;
    private int furnace;
    private int brewing;

    /** 受け取る側で使う(中身は fromBytes で読む) */
    public PacketMultipliers() {
    }

    public PacketMultipliers(final int[] values) {
        this.crafting = values[0];
        this.furnace = values[1];
        this.brewing = values[2];
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.crafting = buf.readInt();
        this.furnace = buf.readInt();
        this.brewing = buf.readInt();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeInt(this.crafting);
        buf.writeInt(this.furnace);
        buf.writeInt(this.brewing);
    }

    /** クライアント側で受け取る */
    public static class Handler implements IMessageHandler<PacketMultipliers, IMessage> {
        @Override
        public IMessage onMessage(final PacketMultipliers message, final MessageContext ctx) {
            ManaitaMultiplier.setFromServer(message.crafting, message.furnace, message.brewing);
            return null;
        }
    }
}

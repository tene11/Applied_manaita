package dev.manaita.appliedmanaita.network;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;

/**
 * サーバー側: 倍率をクライアントへ送る。
 * <ul>
 *     <li>プレイヤーが入ったとき、そのプレイヤーへ</li>
 *     <li>設定ファイルが書き換わって倍率が変わったとき、全員へ(1秒ごとに確認)</li>
 * </ul>
 */
public final class MultiplierSync {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private static int ticks = 0;
    @Nullable
    private static int[] lastSent = null;

    private MultiplierSync() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    MultipliersPacket.of(ManaitaMultiplier.getServerValues()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks < CHECK_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        final int[] now = ManaitaMultiplier.getServerValues();
        if (!Arrays.equals(now, lastSent)) {
            if (lastSent != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), MultipliersPacket.of(now));
            }
            lastSent = now;
        }
    }
}

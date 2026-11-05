package dev.manaita.appliedmanaita.network;

import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;

/**
 * サーバー側: 倍率をクライアントへ送る。
 * <ul>
 *     <li>プレイヤーが入ったとき、そのプレイヤーへ</li>
 *     <li>設定ファイルが書き換わって倍率が変わったとき、全員へ(1秒ごとに確認)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = AppliedManaita.MOD_ID)
public final class MultiplierSync {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private static int ticks = 0;
    private static int[] lastSent = null;

    private MultiplierSync() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            ModNetwork.CHANNEL.sendTo(new PacketMultipliers(ManaitaMultiplier.getServerValues()), (EntityPlayerMP) event.player);
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
                ModNetwork.CHANNEL.sendToAll(new PacketMultipliers(now));
            }
            lastSent = now;
        }
    }
}

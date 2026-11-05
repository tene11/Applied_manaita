package dev.manaita.appliedmanaita.client;

import appeng.api.util.AEColor;
import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.ModRegistry;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * まな板パターンターミナルのアイテムの画面部分に、AE2の標準色(透明ケーブルの色)を付ける。
 * アイテムの見た目は AE2 のパターンターミナルのモデルを使うので、tintindex 1〜4 を AE2 と同じ色にする。
 * (ケーブルに付けたときの色は AE2 がケーブルの色に合わせて付ける)
 */
@Mod.EventBusSubscriber(modid = AppliedManaita.MOD_ID, value = Side.CLIENT)
public final class ModColors {

    private ModColors() {
    }

    static int tint(final int tintIndex) {
        // 1:暗い 2:中間 3:明るい 4:中間〜明るい (AE2のターミナルと同じ決まり)
        return tintIndex > 0 ? AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex) : 0xFFFFFF;
    }

    @SubscribeEvent
    public static void onItemColors(final ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> tint(tintIndex), ModRegistry.MANAITA_PATTERN_TERMINAL);
    }
}

package dev.manaita.appliedmanaita.part;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * まな板パターンターミナルのアイテム。AE2のターミナルと同じく、ケーブルやブロックの面に右クリックで取り付ける。
 */
public class ItemPartManaitaPatternTerminal extends Item implements IPartItem<PartManaitaPatternTerminal> {

    @Nullable
    @Override
    public PartManaitaPatternTerminal createPartFromItemStack(final ItemStack is) {
        return new PartManaitaPatternTerminal(is);
    }

    @Override
    public EnumActionResult onItemUse(final EntityPlayer player, final World world, final BlockPos pos, final EnumHand hand,
                                      final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        return AEApi.instance().partHelper().placeBus(player.getHeldItem(hand), pos, side, player, hand, world);
    }
}

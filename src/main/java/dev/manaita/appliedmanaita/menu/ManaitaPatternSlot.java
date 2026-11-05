package dev.manaita.appliedmanaita.menu;

import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;

import dev.manaita.appliedmanaita.crafting.PatternUtil;

/**
 * まな板分子組立機のパターン欄。入るのは まな板クラフトパターン だけ(見た目は AE2 の分子組立機のパターン欄と同じ)。
 */
public class ManaitaPatternSlot extends RestrictedInputSlot {

    public ManaitaPatternSlot(final InternalInventory inv, final int invSlot) {
        super(PlacableItemType.MOLECULAR_ASSEMBLER_PATTERN, inv, invSlot);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return PatternUtil.isManaitaCraftingPattern(stack) && this.getInventory().isItemValid(this.getSlotIndex(), stack);
    }
}

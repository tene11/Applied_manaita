package dev.manaita.appliedmanaita.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.client.Point;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.IOptionalSlot;

import dev.manaita.appliedmanaita.crafting.ManaitaCraftingPattern;

/**
 * まな板分子組立機の素材欄の1マス。AE2 の分子組立機の素材欄と同じく、
 * パターン欄のレシピで使うマスだけが表示され、レシピに合うものだけを入れられる。
 */
public class ManaitaAssemblerGridSlot extends AppEngSlot implements IOptionalSlot {

    private final ManaitaAssemblerMenu menu;

    public ManaitaAssemblerGridSlot(final ManaitaAssemblerMenu menu, final InternalInventory inv, final int invSlot) {
        super(inv, invSlot);
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return super.mayPlace(stack) && this.menu.isValidItemForSlot(this.getSlotIndex(), stack);
    }

    @Override
    public boolean mayPickup(final Player player) {
        // CPU の仕事中は素材を抜けない(抜くと CPU が完成品を待ち続けてしまう)
        return this.menu.getHost().canExtractGrid() && super.mayPickup(player);
    }

    @Override
    protected boolean getCurrentValidationState() {
        final ItemStack stack = this.getItem();
        return stack.isEmpty() || this.mayPlace(stack);
    }

    @Override
    public boolean isRenderDisabled() {
        return true; // 背景の画像にマスの絵が入っていないため(AE2 の分子組立機と同じ)
    }

    @Override
    public boolean isSlotEnabled() {
        // 何か入っているときは、取り出せるように必ず出す
        final int index = this.getSlotIndex();
        if (!this.getInventory().getStackInSlot(index).isEmpty()) {
            return true;
        }
        final ManaitaCraftingPattern pattern = this.menu.getHost().getPlan();
        return index >= 0 && index < 9 && pattern != null && pattern.isSlotEnabled(index);
    }

    @Override
    public Point getBackgroundPos() {
        return new Point(this.x - 1, this.y - 1);
    }
}

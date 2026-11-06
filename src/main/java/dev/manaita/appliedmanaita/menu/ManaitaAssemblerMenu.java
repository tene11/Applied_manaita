package dev.manaita.appliedmanaita.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.OutputSlot;

import dev.manaita.appliedmanaita.blockentity.ManaitaAssemblerBlockEntity;

/**
 * まな板分子組立機の画面(サーバー・クライアント共通の部分)。
 * 並びは AE2 の分子組立機と同じ: 3x3 の素材・パターン欄・出口・加速カード欄。
 *
 * @see dev.manaita.appliedmanaita.client.ManaitaAssemblerScreen
 */
public class ManaitaAssemblerMenu extends UpgradeableMenu<ManaitaAssemblerBlockEntity> implements IProgressProvider {

    public static final MenuType<ManaitaAssemblerMenu> TYPE =
            MenuTypeHelper.create(ManaitaAssemblerBlockEntity.class, ManaitaAssemblerMenu::new);

    private final ManaitaAssemblerBlockEntity assembler;

    @GuiSync(4)
    public int craftProgress = 0;

    /** 作業まな板の倍率(サーバーの値を画面に出すため) */
    @GuiSync(7)
    public int multiplier = 1;

    private Slot patternSlot;

    public ManaitaAssemblerMenu(final int id, final Inventory playerInv, final ManaitaAssemblerBlockEntity be) {
        super(TYPE, id, playerInv, be);
        this.assembler = be;
    }

    @Override
    protected void setupConfig() {
        final InternalInventory inv = this.getHost().getSubInventory(ManaitaAssemblerBlockEntity.INV_MAIN);

        for (int i = 0; i < ManaitaAssemblerBlockEntity.GRID_SIZE; i++) {
            this.addSlot(new ManaitaAssemblerGridSlot(this, inv, i), SlotSemantics.MACHINE_CRAFTING_GRID);
        }
        this.patternSlot = this.addSlot(new ManaitaPatternSlot(inv, ManaitaAssemblerBlockEntity.GRID_SIZE + 1),
                SlotSemantics.ENCODED_PATTERN);
        this.addSlot(new OutputSlot(inv, ManaitaAssemblerBlockEntity.OUTPUT_SLOT, null), SlotSemantics.MACHINE_OUTPUT);
    }

    /** 素材欄のこのマスに入れてよいか */
    public boolean isValidItemForSlot(final int slot, final ItemStack stack) {
        return this.assembler.canInsertGrid(slot, stack);
    }

    @Override
    public void broadcastChanges() {
        if (this.isServerSide()) {
            this.craftProgress = this.assembler.getCraftingProgress();
            this.multiplier = this.assembler.getMultiplier();
        }
        super.broadcastChanges();
    }

    @Override
    public int getCurrentProgress() {
        return this.craftProgress;
    }

    @Override
    public int getMaxProgress() {
        return ManaitaAssemblerBlockEntity.PROGRESS_MAX;
    }

    public int getMultiplier() {
        return this.multiplier;
    }

    @Override
    public void onSlotChange(final Slot s) {
        // パターンが変わったら、素材欄の「入れてよいか」を確かめ直す
        if (s == this.patternSlot) {
            for (final Slot other : this.slots) {
                if (other != s && other instanceof AppEngSlot appEngSlot) {
                    appEngSlot.resetCachedValidation();
                }
            }
        }
    }
}

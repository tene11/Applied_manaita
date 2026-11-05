package dev.manaita.appliedmanaita.container;

import dev.manaita.appliedmanaita.tile.TileManaitaAssembler;
import dev.manaita.appliedmanaita.crafting.PatternUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * まな板分子組立機の画面(サーバー・クライアント共通)。スロットの位置は AE2 の分子組立機と同じ。
 * <pre>
 * スロット番号
 *   0 - 8  : 素材(3x3)
 *   9      : パターン欄
 *   10     : 出口
 *   11 - 15: 加速カード
 *   16 - 51: プレイヤーのインベントリ
 * </pre>
 */
public class ContainerManaitaAssembler extends Container {

    public static final int GRID_START = 0;
    public static final int GRID_END = 9;
    public static final int SLOT_PATTERN = 9;
    public static final int SLOT_OUTPUT = 10;
    public static final int UPGRADE_START = 11;
    public static final int UPGRADE_END = 16;
    public static final int PLAYER_START = 16;
    public static final int PLAYER_END = 52;

    /** 画面の大きさ・位置(AE2の分子組立機と同じ) */
    public static final int HEIGHT = 197;
    public static final int PROGRESS_X = 148;
    public static final int PROGRESS_Y = 48;

    private final TileManaitaAssembler tile;
    private int lastProgress = -1;
    private int clientProgress = 0;
    /** 素材欄から取り出せるか(MEの仕事中は不可)。クライアント側の表示用に同期する */
    private int lastCanExtract = -1;
    private boolean clientCanExtract = true;

    public ContainerManaitaAssembler(final InventoryPlayer playerInv, final TileManaitaAssembler tile) {
        this.tile = tile;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addSlotToContainer(new SlotGrid(this, x + y * 3, 29 + x * 18, 30 + y * 18));
            }
        }

        this.addSlotToContainer(new SlotItemHandler(tile.getPatternInv(), 0, 126, 16));
        this.addSlotToContainer(new SlotItemHandler(tile.getOutput(), 0, 126, 48));

        for (int i = 0; i < TileManaitaAssembler.UPGRADE_SLOTS; i++) {
            this.addSlotToContainer(new SlotItemHandler(tile.getUpgrades(), i, 187, 8 + i * 18));
        }

        final int playerY = HEIGHT - 82;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlotToContainer(new Slot(playerInv, x + y * 9 + 9, 8 + x * 18, playerY + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            this.addSlotToContainer(new Slot(playerInv, x, 8 + x * 18, playerY + 58));
        }
    }

    public TileManaitaAssembler getTile() {
        return this.tile;
    }

    /** 進み具合 0〜100 (クライアント側) */
    public int getProgress() {
        return this.clientProgress;
    }

    @Override
    public boolean canInteractWith(@Nonnull final EntityPlayer player) {
        return !this.tile.isInvalid()
                && player.getDistanceSq(this.tile.getPos().getX() + 0.5, this.tile.getPos().getY() + 0.5,
                this.tile.getPos().getZ() + 0.5) <= 64.0;
    }

    // ---------------------------------------------------------------- 進み具合の同期

    @Override
    public void addListener(final IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, 0, this.tile.getProgress());
        listener.sendWindowProperty(this, 1, this.tile.canExtractGrid() ? 1 : 0);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        final int now = this.tile.getProgress();
        if (now != this.lastProgress) {
            this.lastProgress = now;
            for (final IContainerListener listener : this.listeners) {
                listener.sendWindowProperty(this, 0, now);
            }
        }
        final int canExtract = this.tile.canExtractGrid() ? 1 : 0;
        if (canExtract != this.lastCanExtract) {
            this.lastCanExtract = canExtract;
            for (final IContainerListener listener : this.listeners) {
                listener.sendWindowProperty(this, 1, canExtract);
            }
        }
    }

    /** 素材欄から取り出せるか(サーバーはタイルから、クライアントは同期された値から) */
    boolean canExtractGrid() {
        final boolean client = this.tile.getWorld() != null && this.tile.getWorld().isRemote;
        return client ? this.clientCanExtract : this.tile.canExtractGrid();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(final int id, final int data) {
        if (id == 0) {
            this.clientProgress = data;
        } else if (id == 1) {
            this.clientCanExtract = data != 0;
        }
    }

    // ---------------------------------------------------------------- シフトクリック

    @Override
    @Nonnull
    public ItemStack transferStackInSlot(@Nonnull final EntityPlayer player, final int index) {
        final Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack() || !slot.canTakeStack(player)) {
            return ItemStack.EMPTY;
        }

        final ItemStack original = slot.getStack().copy();
        final ItemStack moving = original.copy();

        if (index < PLAYER_START) {
            // 組立機 → プレイヤー
            this.mergeItemStack(moving, PLAYER_START, PLAYER_END, true);
        } else if (TileManaitaAssembler.isSpeedCard(moving)) {
            // 加速カード: 空いている枠へ1枚ずつ、入るだけまとめて入れる(AE2と同じ)
            for (int i = UPGRADE_START; i < UPGRADE_END && !moving.isEmpty(); i++) {
                final Slot target = this.inventorySlots.get(i);
                if (!target.getHasStack() && target.isItemValid(moving)) {
                    target.putStack(moving.splitStack(1));
                }
            }
        } else if (PatternUtil.isManaitaPattern(moving)) {
            this.mergeItemStack(moving, SLOT_PATTERN, SLOT_PATTERN + 1, false);
        } else {
            // 素材: 入れられるマスへ1個ずつ
            for (int i = GRID_START; i < GRID_END && !moving.isEmpty(); i++) {
                final Slot target = this.inventorySlots.get(i);
                if (!target.getHasStack() && target.isItemValid(moving)) {
                    target.putStack(moving.splitStack(1));
                }
            }
        }

        final int moved = original.getCount() - moving.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        if (index < PLAYER_START) {
            slot.decrStackSize(moved);
        } else {
            slot.getStack().shrink(moved);
            if (slot.getStack().isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            }
        }
        slot.onSlotChanged();
        return ItemStack.EMPTY; // 1回だけ動かす(出口から大量に出るのを防ぐ)
    }

    /** 素材欄: パターン欄のレシピに合うものだけ入る。CPUの仕事中は取り出せない */
    public static class SlotGrid extends SlotItemHandler {
        private final ContainerManaitaAssembler container;

        public SlotGrid(final ContainerManaitaAssembler container, final int index, final int x, final int y) {
            super(container.getTile().getGrid(), index, x, y);
            this.container = container;
        }

        @Override
        public boolean isItemValid(@Nonnull final ItemStack stack) {
            return this.container.canExtractGrid() && this.container.getTile().canInsertGrid(this.getSlotIndex(), stack);
        }

        @Override
        public boolean canTakeStack(final EntityPlayer player) {
            return this.container.canExtractGrid();
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        @Override
        public int getItemStackLimit(@Nonnull final ItemStack stack) {
            return 1;
        }
    }
}

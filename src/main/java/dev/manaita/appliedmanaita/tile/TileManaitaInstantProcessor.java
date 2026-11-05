package dev.manaita.appliedmanaita.tile;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkTile;
import appeng.util.item.AEItemStack;
import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.crafting.IManaitaPatternDetails;
import io.netty.buffer.ByteBuf;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MEインターフェースの隣に置いて、加工用のまな板パターンを一瞬で仕上げる機械の共通部分。
 * まな板分子精錬機({@link TileManaitaMolecularSmelter})・まな板分子醸造機({@link TileManaitaMolecularBrewer})がこれを継承し、
 * どのパターンを受け取るかだけを決める。
 * <p>
 * インターフェースから素材を受け取ると、燃料なしで一瞬で仕上げ、
 * まな板+ の倍率をかけた数の完成品を ME ネットワークに直接入れる(CPUはそれを待っている)。
 * <p>
 * 受け取ったその場ではネットワークに入れず、次のtickで入れる。
 * (AE2 の CPU は「機械に渡し終わってから」完成品を待つ登録をするので、渡している最中に入れると
 *  CPU が受け取れず、ただの倉庫行きになってしまう)
 * <p>
 * 燃料は要らないが、1回ごとに AE の電力を使う。電力が足りないときは仕事を受けない。
 * CPU の並列数(コプロセッサー)の分だけ、1tickに何回でも受け取れる。
 */
public abstract class TileManaitaInstantProcessor extends AENetworkTile implements ICraftingMachine, IGridTickable {

    /** 1回仕上げるのに使う電力(AE)。AE2 の分子組立機で加速カードなしの1回分と同じくらい */
    public static final double POWER_PER_OPERATION = 100.0;

    /** 出力待ちの種類がこれ以上たまったら、出し切るまで受け付けない(ネットワークが満杯のときの保険) */
    private static final int MAX_PENDING = 64;

    /** 壊したときに落とすスタック数の上限(アイテムが多すぎてサーバーが止まらないように) */
    private static final int MAX_DROP_STACKS = 1000;

    /** ネットワークへ入れる完成品(倍率込み)。同じアイテムはまとめる。個数は long */
    private final List<IAEItemStack> pending = new ArrayList<>();

    /** ネットワークに入りきらなかった。出し切るまで仕事を受けない */
    private boolean stuck = false;

    /** 電源が入っているか(まな板分子組立機と同じく、光るモデルの切り替えに使う。クライアントへ同期する) */
    private boolean isPowered = false;

    private final IActionSource source = new MachineSource(this);

    protected TileManaitaInstantProcessor() {
        this.getProxy().setIdlePowerUsage(0.0);
    }

    /** このパターンを受け取るか(かまどならかまど用、醸造台なら醸造用のまな板パターン) */
    protected abstract boolean acceptsPattern(ICraftingPatternDetails details);

    /** debug.log に出す名前 */
    protected abstract String logName();

    // ---------------------------------------------------------------- ICraftingMachine (MEインターフェースから)

    @Override
    public boolean acceptsPlans() {
        return !this.stuck && this.pending.size() < MAX_PENDING && this.getProxy().isActive();
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table, final EnumFacing ejectionDirection) {
        if (!(patternDetails instanceof IManaitaPatternDetails) || !this.acceptsPattern(patternDetails) || !this.acceptsPlans()) {
            return false;
        }
        final IManaitaPatternDetails details = (IManaitaPatternDetails) patternDetails;

        // 渡された素材がパターンどおりか(種類・数とも)確かめる(違えば断る。断ると CPU が素材をネットワークへ戻す)
        if (!this.matchesInputs(details, table)) {
            return false;
        }

        // 電力(足りなければ断る)
        try {
            final IEnergyGrid energy = this.getProxy().getEnergy();
            final double available = energy.extractAEPower(POWER_PER_OPERATION, Actionable.SIMULATE, PowerMultiplier.CONFIG);
            if (available < POWER_PER_OPERATION - 0.01) {
                return false;
            }
            energy.extractAEPower(POWER_PER_OPERATION, Actionable.MODULATE, PowerMultiplier.CONFIG);
        } catch (final GridAccessException e) {
            return false;
        }

        // 仕上げる(一瞬)。CPU が待つのは、この瞬間の倍率での出力
        for (final IAEItemStack out : details.getCondensedOutputs()) {
            if (out != null) {
                this.addPending(out.copy());
            }
        }
        // 素材は使い切ったので消す。バケツなどの「使った後に残るアイテム」はネットワークへ返す
        for (int i = 0; i < table.getSizeInventory(); i++) {
            final ItemStack stack = table.getStackInSlot(i);
            if (!stack.isEmpty()) {
                final ItemStack remain = ForgeHooks.getContainerItem(stack);
                if (!remain.isEmpty()) {
                    final IAEItemStack leftover = AEItemStack.fromItemStack(remain);
                    if (leftover != null) {
                        leftover.setStackSize((long) remain.getCount() * stack.getCount());
                        this.addPending(leftover);
                    }
                }
            }
            table.setInventorySlotContents(i, ItemStack.EMPTY);
        }

        this.saveChanges();
        this.wake();
        return true;
    }

    /** 渡された素材が、パターンの素材(種類ごとの合計数)とぴったり同じか */
    private boolean matchesInputs(final IManaitaPatternDetails details, final InventoryCrafting table) {
        final List<IAEItemStack> given = new ArrayList<>();
        outer:
        for (int i = 0; i < table.getSizeInventory(); i++) {
            final ItemStack stack = table.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (final IAEItemStack g : given) {
                if (g.isSameType(stack)) {
                    g.incStackSize(stack.getCount());
                    continue outer;
                }
            }
            given.add(AEItemStack.fromItemStack(stack));
        }

        int wantedKinds = 0;
        for (final IAEItemStack wanted : details.getCondensedInputs()) {
            if (wanted == null) {
                continue;
            }
            wantedKinds++;
            boolean found = false;
            for (final IAEItemStack g : given) {
                if (g.isSameType(wanted)) {
                    found = g.getStackSize() == wanted.getStackSize();
                    break;
                }
            }
            if (!found) {
                AppliedManaita.LOGGER.debug("[{} {}] 素材がパターンと違うので断りました: 必要 {} / 渡された {}",
                        this.logName(), this.pos, wanted, given);
                return false;
            }
        }
        if (wantedKinds != given.size()) {
            AppliedManaita.LOGGER.debug("[{} {}] パターンにない素材があるので断りました: {}", this.logName(), this.pos, given);
            return false;
        }
        return true;
    }

    private void addPending(final IAEItemStack stack) {
        for (final IAEItemStack p : this.pending) {
            if (p.equals(stack)) {
                p.incStackSize(stack.getStackSize());
                return;
            }
        }
        this.pending.add(stack);
    }

    // ---------------------------------------------------------------- tick

    @Override
    @Nonnull
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return new TickingRequest(1, 1, this.pending.isEmpty(), false);
    }

    @Override
    @Nonnull
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        this.flushToNetwork();
        if (this.pending.isEmpty()) {
            this.stuck = false;
            return TickRateModulation.SLEEP;
        }
        this.stuck = true; // 入りきらない。出し切るまで次の仕事は受けない
        return TickRateModulation.SLOWER;
    }

    /** ME ネットワークへ入れる。入りきらなかった分は残す */
    private void flushToNetwork() {
        if (this.pending.isEmpty()) {
            return;
        }
        try {
            final IMEMonitor<IAEItemStack> inv = this.getProxy().getStorage()
                    .getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));

            final Iterator<IAEItemStack> it = this.pending.iterator();
            while (it.hasNext()) {
                final IAEItemStack stack = it.next();
                final IAEItemStack left = inv.injectItems(stack.copy(), Actionable.MODULATE, this.source);
                if (left == null || left.getStackSize() <= 0) {
                    it.remove();
                } else {
                    stack.setStackSize(left.getStackSize());
                }
            }
            this.saveChanges();
        } catch (final GridAccessException e) {
            // ネットワーク未接続。つながったら入れる
        }
    }

    private void wake() {
        try {
            this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
        } catch (final GridAccessException e) {
            // ネットワーク未接続
        }
    }

    // ---------------------------------------------------------------- AE2

    @Override
    @Nonnull
    public AECableType getCableConnectionType(@Nonnull final AEPartLocation dir) {
        return AECableType.COVERED;
    }

    // ---------------------------------------------------------------- 電源の状態(見た目用)

    public boolean isPowered() {
        return this.isPowered;
    }

    @MENetworkEventSubscribe
    public void onPowerEvent(final MENetworkPowerStatusChange event) {
        this.updatePowerState();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.updatePowerState();
        if (!this.pending.isEmpty()) {
            this.wake(); // 読み込み直後に残っていた出力待ちを出す
        }
    }

    private void updatePowerState() {
        boolean now = false;
        try {
            now = this.getProxy().isActive()
                    && this.getProxy().getEnergy().extractAEPower(1, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0001;
        } catch (final GridAccessException ignored) {
            // ネットワーク未接続
        }
        if (now != this.isPowered) {
            this.isPowered = now;
            this.markForUpdate();
        }
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.isPowered);
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        final boolean old = this.isPowered;
        this.isPowered = data.readBoolean();
        return changed || old != this.isPowered;
    }

    // ---------------------------------------------------------------- 保存

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        final NBTTagList pendingTag = new NBTTagList();
        for (final IAEItemStack stack : this.pending) {
            final NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            pendingTag.appendTag(tag);
        }
        data.setTag("pending", pendingTag);
        return data;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.pending.clear();
        final NBTTagList pendingTag = data.getTagList("pending", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < pendingTag.tagCount(); i++) {
            final IAEItemStack stack = AEItemStack.fromNBT(pendingTag.getCompoundTagAt(i));
            if (stack != null) {
                this.pending.add(stack);
            }
        }
    }

    /** 壊したときに落とすもの。ネットワークに入れられなかった完成品は、アイテムとして落とす */
    public List<ItemStack> getContentsForDrop() {
        final List<ItemStack> drops = new ArrayList<>();
        for (final IAEItemStack stack : this.pending) {
            long left = stack.getStackSize();
            final ItemStack base = stack.createItemStack();
            final int max = Math.max(1, base.getMaxStackSize());
            while (left > 0 && drops.size() < MAX_DROP_STACKS) { // 極端な数でワールドが止まらないよう上限を設ける
                final ItemStack chunk = base.copy();
                final int n = (int) Math.min(max, left);
                chunk.setCount(n);
                drops.add(chunk);
                left -= n;
            }
            if (left > 0) {
                AppliedManaita.LOGGER.warn("[{} {}] 壊したときに出力待ちが多すぎたため、{} を {} 個落とせませんでした",
                        this.logName(), this.pos, base.getDisplayName(), left);
            }
        }
        this.pending.clear();
        return drops;
    }
}

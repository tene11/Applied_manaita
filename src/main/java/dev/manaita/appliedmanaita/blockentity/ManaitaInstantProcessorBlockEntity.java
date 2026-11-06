package dev.manaita.appliedmanaita.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.capabilities.Capabilities;

import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.crafting.IManaitaPattern;
import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import dev.manaita.appliedmanaita.crafting.ManaitaProcessingPattern;

/**
 * パターンプロバイダーの隣に置いて、まな板加工パターンを一瞬で仕上げる機械。
 * まな板分子精錬機(かまどレシピ)・まな板分子醸造機(醸造レシピ)の2つがこのクラスで、
 * どちらのパターンを受け取るかは {@link #kind} で決まる。
 * <p>
 * プロバイダーから素材を受け取ると、燃料なしで一瞬で仕上げ、
 * まな板mod の倍率をかけた数の完成品を ME ネットワークに直接入れる(CPU はそれを待っている)。
 * <p>
 * 受け取ったその場ではネットワークに入れず、次のtickで入れる。
 * (AE2 の CPU は「機械に渡し終わってから」完成品を待つ登録をするので、渡している最中に入れると
 *  CPU が受け取れず、ただの倉庫行きになってしまう)
 * <p>
 * 燃料は要らないが、1回ごとに AE の電力を使う。電力が足りないときは仕事を受けない。
 * CPU の並列数(コプロセッサー)の分だけ、1tickに何回でも受け取れる。
 */
public class ManaitaInstantProcessorBlockEntity extends AENetworkBlockEntity
        implements ICraftingMachine, IGridTickable, IPowerChannelState {

    /** 1回仕上げるのに使う電力(AE)。AE2 の分子組立機で加速カードなしの1回分と同じくらい */
    public static final double POWER_PER_OPERATION = 100.0;

    /** 出力待ちの種類がこれ以上たまったら、出し切るまで受け付けない(ネットワークが満杯のときの保険) */
    private static final int MAX_PENDING = 64;

    /** 壊したときに落とすスタック数の上限(アイテムが多すぎてサーバーが止まらないように) */
    private static final int MAX_DROP_STACKS = 1000;

    /** どのパターンを受け取るか(FURNACE: まな板分子精錬機 / BREWING: まな板分子醸造機) */
    private final ManaitaMultiplier.Kind kind;

    /** ネットワークへ入れる完成品(倍率込み)。同じものはまとめる。個数は long */
    private final List<GenericStack> pending = new ArrayList<>();

    /** ネットワークに入りきらなかった。出し切るまで仕事を受けない */
    private boolean stuck = false;

    /** 電源が入っているか(まな板分子組立機と同じく、光る見た目の切り替えに使う。クライアントへ同期する) */
    private boolean isPowered = false;

    private final IActionSource source = IActionSource.ofMachine(this);

    public ManaitaInstantProcessorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                              final ManaitaMultiplier.Kind kind) {
        super(type, pos, state);
        this.kind = kind;
        this.getMainNode()
                .setIdlePowerUsage(0.0)
                .addService(IGridTickable.class, this);
    }

    public ManaitaMultiplier.Kind getKind() {
        return this.kind;
    }

    private String logName() {
        return this.kind == ManaitaMultiplier.Kind.BREWING ? "まな板分子醸造機" : "まな板分子精錬機";
    }

    // ---------------------------------------------------------------- ICraftingMachine (パターンプロバイダーから)

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        final var item = this.getBlockState().getBlock().asItem();
        final Component name = this.hasCustomName() ? this.getCustomName() : item.getDescription();
        return new PatternContainerGroup(AEItemKey.of(item), name, List.of());
    }

    @Override
    public boolean acceptsPlans() {
        return !this.stuck && this.pending.size() < MAX_PENDING && this.getMainNode().isActive();
    }

    @Override
    public boolean pushPattern(final IPatternDetails patternDetails, final KeyCounter[] inputs, final Direction ejectionDirection) {
        if (!(patternDetails instanceof ManaitaProcessingPattern pattern) || pattern.getKind() != this.kind
                || !this.acceptsPlans()) {
            return false;
        }

        // 渡された素材がパターンどおりか(種類・数とも)確かめる(違えば断る。断ると CPU が素材を持ったまま別の機械を探す)
        if (!this.matchesInputs(pattern, inputs)) {
            return false;
        }

        // 電力(足りなければ断る)
        final var grid = this.getMainNode().getGrid();
        if (grid == null) {
            return false;
        }
        final var energy = grid.getEnergyService();
        if (energy.extractAEPower(POWER_PER_OPERATION, Actionable.SIMULATE, PowerMultiplier.CONFIG) < POWER_PER_OPERATION - 0.01) {
            return false;
        }
        energy.extractAEPower(POWER_PER_OPERATION, Actionable.MODULATE, PowerMultiplier.CONFIG);

        // 仕上げる(一瞬)。CPU が待つのは、この瞬間の倍率での出力
        for (final GenericStack out : pattern.getOutputs()) {
            this.addPending(out);
        }
        // 素材は使い切ったので消す。バケツなどの「使った後に残るアイテム」はネットワークへ返す
        for (final KeyCounter list : inputs) {
            for (final var entry : list) {
                if (entry.getKey() instanceof AEItemKey itemKey && entry.getLongValue() > 0) {
                    final ItemStack one = itemKey.toStack();
                    if (one.hasCraftingRemainingItem()) {
                        final ItemStack remain = one.getCraftingRemainingItem();
                        final AEItemKey remainKey = AEItemKey.of(remain);
                        if (remainKey != null) {
                            this.addPending(new GenericStack(remainKey, (long) remain.getCount() * entry.getLongValue()));
                        }
                    }
                }
            }
            list.clear();
        }

        this.saveChanges();
        this.wake();
        return true;
    }

    /** 渡された素材が、パターンの素材(種類ごとの合計数)とぴったり同じか */
    private boolean matchesInputs(final IManaitaPattern pattern, final KeyCounter[] inputs) {
        final IPatternDetails.IInput[] wanted = pattern.getInputs();
        if (inputs.length != wanted.length) {
            return false;
        }
        for (int i = 0; i < wanted.length; i++) {
            final GenericStack template = wanted[i].getPossibleInputs()[0];
            final long amount = template.amount() * wanted[i].getMultiplier();
            long given = 0;
            for (final var entry : inputs[i]) {
                final AEKey key = entry.getKey();
                if (entry.getLongValue() == 0) {
                    continue;
                }
                if (!key.equals(template.what())) {
                    AppliedManaita.LOGGER.debug("[{} {}] パターンにない素材があるので断りました: {}", this.logName(), this.worldPosition, key);
                    return false;
                }
                given += entry.getLongValue();
            }
            if (given != amount) {
                AppliedManaita.LOGGER.debug("[{} {}] 素材の数がパターンと違うので断りました: 必要 {} x {} / 渡された {}",
                        this.logName(), this.worldPosition, template.what(), amount, given);
                return false;
            }
        }
        return true;
    }

    private void addPending(final GenericStack stack) {
        if (stack.amount() <= 0) {
            return;
        }
        for (int i = 0; i < this.pending.size(); i++) {
            final GenericStack p = this.pending.get(i);
            if (p.what().equals(stack.what())) {
                final long sum = p.amount() + stack.amount();
                this.pending.set(i, new GenericStack(p.what(), sum < 0 ? Long.MAX_VALUE : sum));
                return;
            }
        }
        this.pending.add(stack);
    }

    // ---------------------------------------------------------------- tick

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(1, 1, this.pending.isEmpty());
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
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
        final var grid = this.getMainNode().getGrid();
        if (grid == null) {
            return; // ネットワーク未接続。つながったら入れる
        }
        final var storage = grid.getStorageService().getInventory();
        for (int i = 0; i < this.pending.size(); ) {
            final GenericStack stack = this.pending.get(i);
            final long inserted = storage.insert(stack.what(), stack.amount(), Actionable.MODULATE, this.source);
            final long left = stack.amount() - inserted;
            if (left <= 0) {
                this.pending.remove(i);
            } else {
                this.pending.set(i, new GenericStack(stack.what(), left));
                i++;
            }
        }
        this.saveChanges();
    }

    private void wake() {
        this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    // ---------------------------------------------------------------- AE2

    @Override
    public AECableType getCableConnectionType(final Direction dir) {
        return AECableType.COVERED;
    }

    /** パターンプロバイダーから「クラフト機械」として見えるようにする */
    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction facing) {
        if (capability == Capabilities.CRAFTING_MACHINE) {
            return Capabilities.CRAFTING_MACHINE.orEmpty(capability, LazyOptional.of(() -> this));
        }
        return super.getCapability(capability, facing);
    }

    // ---------------------------------------------------------------- 電源の状態(見た目用)

    @Override
    public void onMainNodeStateChanged(final IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            boolean now = false;
            final var grid = this.getMainNode().getGrid();
            if (grid != null) {
                now = this.getMainNode().isPowered() && grid.getEnergyService()
                        .extractAEPower(1, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0001;
            }
            if (now != this.isPowered) {
                this.isPowered = now;
                this.markForUpdate();
            }
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        if (!this.pending.isEmpty()) {
            this.wake(); // 読み込み直後に残っていた出力待ちを出す
        }
    }

    @Override
    public boolean isPowered() {
        return this.isPowered;
    }

    @Override
    public boolean isActive() {
        return this.isPowered;
    }

    @Override
    protected boolean readFromStream(final FriendlyByteBuf data) {
        final boolean changed = super.readFromStream(data);
        final boolean old = this.isPowered;
        this.isPowered = data.readBoolean();
        return changed || old != this.isPowered;
    }

    @Override
    protected void writeToStream(final FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isPowered);
    }

    // ---------------------------------------------------------------- 保存

    @Override
    public void saveAdditional(final CompoundTag data) {
        super.saveAdditional(data);
        final ListTag list = new ListTag();
        for (final GenericStack stack : this.pending) {
            list.add(GenericStack.writeTag(stack));
        }
        data.put("pending", list);
    }

    @Override
    public void loadTag(final CompoundTag data) {
        super.loadTag(data);
        this.pending.clear();
        final ListTag list = data.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final GenericStack stack = GenericStack.readTag(list.getCompound(i));
            if (stack != null) {
                this.pending.add(stack);
            }
        }
    }

    // ---------------------------------------------------------------- 破壊時

    /** 壊したときに落とすもの。ネットワークに入れられなかった完成品は、アイテムとして落とす */
    @Override
    public void addAdditionalDrops(final Level level, final BlockPos pos, final List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (final GenericStack stack : this.pending) {
            if (!(stack.what() instanceof AEItemKey key)) {
                continue;
            }
            long left = stack.amount();
            final int max = Math.max(1, key.toStack().getMaxStackSize());
            while (left > 0 && drops.size() < MAX_DROP_STACKS) {
                final int n = (int) Math.min(max, left);
                drops.add(key.toStack(n));
                left -= n;
            }
            if (left > 0) {
                AppliedManaita.LOGGER.warn("[{} {}] 壊したときに出力待ちが多すぎたため、{} を {} 個落とせませんでした",
                        this.logName(), pos, key, left);
            }
        }
        this.pending.clear();
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.pending.clear();
    }
}

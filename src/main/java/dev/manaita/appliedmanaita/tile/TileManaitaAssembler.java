package dev.manaita.appliedmanaita.tile;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Upgrades;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
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
import appeng.api.util.IConfigManager;
import appeng.util.ConfigManager;
import appeng.container.ContainerNull;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkTile;
import appeng.util.item.AEItemStack;
import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.ModRegistry;
import dev.manaita.appliedmanaita.crafting.ManaitaPatternDetails;
import dev.manaita.appliedmanaita.crafting.PatternUtil;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * まな板分子組立機。AE2 の分子組立機と同じく、2通りの使い方ができる。
 * <ol>
 *     <li><b>MEインターフェースの隣に置く</b>: インターフェースに入っている「まな板パターン」の素材を受け取り、
 *     倍率をかけた数のクラフト結果を ME ネットワークに直接入れる(CPUはそれを待っている)。</li>
 *     <li><b>単体で使う</b>: 画面のパターン欄にまな板パターンを入れ、ホッパーなどで素材を入れると、
 *     クラフトして隣のインベントリへ出す。隣に入らない分は出口に残り、ホッパーや画面から取り出せる。</li>
 * </ol>
 * 加速カード(最大5枚)の効果と電力の割増は AE2 の分子組立機と同じ。
 */
public class TileManaitaAssembler extends AENetworkTile implements ICraftingMachine, IGridTickable, IUpgradeableHost {

    public static final int GRID_SIZE = 9;
    public static final int UPGRADE_SLOTS = 5;
    public static final int PROGRESS_MAX = 100;

    /** AE2 の分子組立機と同じ: 加速カードの枚数ごとの「1tickの進み」と「電力の割増」 */
    private static final int[] SPEED = {10, 13, 17, 20, 25, 50};
    private static final double[] POWER_TAX = {1.0, 1.3, 1.7, 2.0, 2.5, 5.0};

    /**
     * 単体動作で、1tickに隣のインベントリへ出すスタック数の上限。
     * 隣が何でも吸い込むブロック(ゴミ箱など)で倍率が極端に大きいと、出し切るまでループしてサーバーが固まるため。
     * 出し切れなかった分は次のtickに回る。
     */
    private static final int MAX_STACKS_PER_FLUSH = 256;

    /** 3x3 の素材。1マス1個まで */
    private final ItemStackHandler grid = new ItemStackHandler(GRID_SIZE) {
        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            TileManaitaAssembler.this.onInventoryChanged();
        }
    };

    /** 単体で使うときのパターン欄 */
    private final ItemStackHandler patternInv = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(final int slot, @Nonnull final ItemStack stack) {
            return PatternUtil.isManaitaPattern(stack);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            TileManaitaAssembler.this.progress = 0;
            TileManaitaAssembler.this.onInventoryChanged();
        }
    };

    /** 加速カード欄。1マス1枚まで */
    private final ItemStackHandler upgrades = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        public boolean isItemValid(final int slot, @Nonnull final ItemStack stack) {
            return isSpeedCard(stack);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(final int slot) {
            TileManaitaAssembler.this.onInventoryChanged();
        }
    };

    /** できたアイテムの出口(画面の出口欄・ホッパーから見える形にしたもの) */
    private final OutputHandler output = new OutputHandler();

    /** 外(ホッパーなど)から見たインベントリ: 0〜8 が素材(入れるだけ)、9 が出口(取り出すだけ) */
    private final IItemHandler external = new ExternalHandler();

    /** 出力待ちのアイテム。個数は long */
    private final List<IAEItemStack> pending = new ArrayList<>();
    /** true: 出力待ちを ME ネットワークへ入れる(CPUの仕事) / false: 隣のインベントリへ出す(単体動作) */
    private boolean pendingToNetwork = false;

    private final IActionSource source = new MachineSource(this);

    /** MEインターフェースから渡された仕事 */
    private ItemStack jobPattern = ItemStack.EMPTY;
    private ManaitaPatternDetails job = null;
    /**
     * 素材を受け取った瞬間の出力(倍率込み)。CPUはこの瞬間の数を「返ってくる数」として記録するので、
     * 作っている途中でコンフィグの倍率が変わっても、この数で作ればCPUとずれない。
     */
    private final List<IAEItemStack> jobOutputs = new ArrayList<>();

    /** パターン欄のパターン(作り直しを減らすためにキャッシュ) */
    private ItemStack cachedPlanStack = ItemStack.EMPTY;
    private ManaitaPatternDetails cachedPlan = null;

    private int progress = 0;

    /**
     * 休んでいた状態から起きた直後か。AE2と同じく、このときは「前回から何tick経ったか」を1として数える。
     * (数えないと、休んでいた時間の分だけ一気に進んで、ゲージがたまる間もなく完成してしまう)
     */
    private boolean reboot = true;

    /** 電源が入っているか(AE2と同じく、光るモデルの切り替えに使う。クライアントへ同期する) */
    private boolean isPowered = false;

    public TileManaitaAssembler() {
        this.getProxy().setIdlePowerUsage(0.0);
    }

    public static boolean isSpeedCard(final ItemStack stack) {
        return !stack.isEmpty() && AEApi.instance().definitions().materials().cardSpeed().isSameAs(stack);
    }

    // ---------------------------------------------------------------- 画面から使うもの

    public ItemStackHandler getGrid() {
        return this.grid;
    }

    public ItemStackHandler getPatternInv() {
        return this.patternInv;
    }

    public ItemStackHandler getUpgrades() {
        return this.upgrades;
    }

    public OutputHandler getOutput() {
        return this.output;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getInstalledSpeedCards() {
        int count = 0;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (isSpeedCard(this.upgrades.getStackInSlot(i))) {
                count++;
            }
        }
        return Math.min(count, SPEED.length - 1);
    }

    /** 素材欄に入れてよいか(パターン欄のレシピに合うものだけ。CPUの仕事中は不可) */
    public boolean canInsertGrid(final int slot, final ItemStack stack) {
        if (!this.jobPattern.isEmpty() || stack.isEmpty()) {
            return false;
        }
        final ManaitaPatternDetails plan = this.getPlan();
        return plan != null && plan.isValidItemForSlot(slot, single(stack), this.world);
    }

    /** 素材欄から取り出してよいか(CPUの仕事中は不可) */
    public boolean canExtractGrid() {
        return this.jobPattern.isEmpty();
    }

    private static ItemStack single(final ItemStack stack) {
        final ItemStack one = stack.copy();
        one.setCount(1);
        return one;
    }

    // ---------------------------------------------------------------- ICraftingMachine (MEインターフェースから)

    @Override
    public boolean acceptsPlans() {
        // AE2と同じく、パターン欄を使っているときはインターフェースの仕事を受けない
        return this.patternInv.getStackInSlot(0).isEmpty()
                && this.jobPattern.isEmpty()
                && this.pending.isEmpty()
                && this.isGridEmpty();
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table, final EnumFacing ejectionDirection) {
        if (!this.acceptsPlans() || !(patternDetails instanceof ManaitaPatternDetails)) {
            AppliedManaita.LOGGER.debug("[組立機 {}] パターンを断りました (受付可={}, まな板パターン={})",
                    this.pos, this.acceptsPlans(), patternDetails instanceof ManaitaPatternDetails);
            return false;
        }

        // CPUから渡される表は3x3(クラフトパターン)。念のため9マスより後ろは出力待ちへ回す
        for (int i = 0; i < table.getSizeInventory(); i++) {
            final ItemStack stack = table.getStackInSlot(i);
            if (i < GRID_SIZE) {
                this.grid.setStackInSlot(i, stack.copy());
            } else if (!stack.isEmpty()) {
                this.pending.add(AEItemStack.fromItemStack(stack)); // 念のため。来ないはず
                this.pendingToNetwork = true;
            }
        }

        this.job = (ManaitaPatternDetails) patternDetails;
        this.jobPattern = patternDetails.getPattern().copy();
        this.jobOutputs.clear();
        for (final IAEItemStack out : patternDetails.getOutputs()) {
            if (out != null) {
                this.jobOutputs.add(out.copy());
            }
        }
        this.progress = 0;
        this.saveChanges();
        this.wake();
        AppliedManaita.LOGGER.debug("[組立機 {}] 素材を受け取りました: {}", this.pos, this.job.getPrimaryOutput());
        return true;
    }

    // ---------------------------------------------------------------- tick

    @Override
    @Nonnull
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        // AE2の分子組立機と同じく毎tick呼んでもらう。
        // (間隔を空けると、まとめて何tick分も進んでゲージが飛び、一瞬で完成したように見える)
        return new TickingRequest(1, 1, !this.hasWork(), false);
    }

    @Override
    @Nonnull
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, int ticksSinceLastCall) {
        if (this.reboot) {
            ticksSinceLastCall = 1;
            this.reboot = false;
        }

        // 1. 出力待ちを出し切るまでは次を作らない
        if (!this.pending.isEmpty()) {
            this.flushPending();
            if (!this.pending.isEmpty()) {
                this.reboot = true; // 待っていた時間はクラフトの進みに数えない
                return TickRateModulation.SLOWER; // 入りきらない。少し待って再挑戦
            }
        }

        // 2. 今やる仕事を決める (インターフェースの仕事が優先)
        final ManaitaPatternDetails plan;
        final boolean forced = !this.jobPattern.isEmpty();
        if (forced) {
            if (this.job == null) {
                // ワールド読み込み直後はNBTからパターンを復元する
                final ICraftingPatternDetails restored = ModRegistry.MANAITA_PATTERN.getPatternForItem(this.jobPattern, this.world);
                if (!(restored instanceof ManaitaPatternDetails)) {
                    this.abortJob();
                    return this.sleep();
                }
                this.job = (ManaitaPatternDetails) restored;
            }
            plan = this.job;
        } else {
            plan = this.getPlan();
            // パターンを抜いた・差し替えたなどで、レシピに合わない素材が残っていたら出口へ出す(AE2と同じ)
            if (this.ejectInvalidGridItems(plan)) {
                this.flushPending();
                return this.hasWork() ? TickRateModulation.FASTER : this.sleep();
            }
            if (plan == null || !this.hasMats(plan)) {
                this.progress = 0;
                return this.sleep(); // 素材が入ったら起こされる
            }
        }

        // 3. 進める(加速カードの枚数で速さと電力が変わる)
        final int cards = this.getInstalledSpeedCards();
        this.progress += this.usePower(ticksSinceLastCall, SPEED[cards], POWER_TAX[cards]);
        if (this.progress >= PROGRESS_MAX) {
            this.progress = 0;
            this.craft(plan, forced);
        }
        this.saveChanges();

        return this.hasWork() ? TickRateModulation.FASTER : this.sleep();
    }

    /** 休む。次に起きたときは1tickから数え直す */
    private TickRateModulation sleep() {
        this.reboot = true;
        return TickRateModulation.SLEEP;
    }

    private boolean hasWork() {
        if (!this.pending.isEmpty() || !this.jobPattern.isEmpty()) {
            return true;
        }
        final ManaitaPatternDetails plan = this.getPlan();
        return this.hasInvalidGridItems(plan) || (plan != null && this.hasMats(plan));
    }

    /** 素材欄に、今のパターンに合わないアイテムがあるか(パターンがないなら、何か入っていれば true) */
    private boolean hasInvalidGridItems(@Nullable final ManaitaPatternDetails plan) {
        for (int i = 0; i < GRID_SIZE; i++) {
            final ItemStack stack = this.grid.getStackInSlot(i);
            if (!stack.isEmpty() && (plan == null || !plan.isValidItemForSlot(i, stack, this.world))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 単体動作で、今のパターンに合わない素材を出力待ちへ移す(隣のインベントリか出口へ出る)。
     * これをしないと素材欄が埋まったままになり、MEインターフェースの仕事も受けられなくなる。
     *
     * @return 何か移したら true
     */
    private boolean ejectInvalidGridItems(@Nullable final ManaitaPatternDetails plan) {
        boolean moved = false;
        for (int i = 0; i < GRID_SIZE; i++) {
            final ItemStack stack = this.grid.getStackInSlot(i);
            if (!stack.isEmpty() && (plan == null || !plan.isValidItemForSlot(i, stack, this.world))) {
                this.pending.add(AEItemStack.fromItemStack(stack));
                this.grid.setStackInSlot(i, ItemStack.EMPTY);
                moved = true;
            }
        }
        if (moved) {
            this.pendingToNetwork = false;
            this.progress = 0;
        }
        return moved;
    }

    /** AE2 と同じ電力の使い方: (進み × 割増) だけ電力を使い、割増で割った分だけ進む */
    private int usePower(final int ticks, final int bonus, final double tax) {
        try {
            final double used = this.getProxy().getEnergy()
                    .extractAEPower(ticks * bonus * tax, Actionable.MODULATE, PowerMultiplier.CONFIG);
            return (int) (used / tax);
        } catch (final GridAccessException e) {
            return 0;
        }
    }

    private boolean hasMats(final ManaitaPatternDetails plan) {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!plan.isValidItemForSlot(i, this.grid.getStackInSlot(i), this.world)) {
                return false;
            }
        }
        return true;
    }

    private boolean isGridEmpty() {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!this.grid.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void craft(final ManaitaPatternDetails plan, final boolean forced) {
        final InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int i = 0; i < GRID_SIZE; i++) {
            ic.setInventorySlotContents(i, this.grid.getStackInSlot(i));
        }

        this.pendingToNetwork = forced;

        final ItemStack check = plan.getOutput(ic, this.world);
        if (check.isEmpty()) {
            // 素材がレシピに合わない。素材は返す
            AppliedManaita.LOGGER.debug("[組立機 {}] 素材がレシピに合わないので中止しました", this.pos);
            this.abortJob();
            return;
        }

        // 出力(倍率込み)。個数は long のまま。
        // CPUの仕事は受け取った瞬間の数で、単体動作は今の倍率で作る
        if (forced && !this.jobOutputs.isEmpty()) {
            for (final IAEItemStack out : this.jobOutputs) {
                this.pending.add(out.copy());
            }
        } else {
            for (final IAEItemStack out : plan.getOutputs()) {
                if (out != null) {
                    this.pending.add(out.copy());
                }
            }
        }

        // バケツなどの「クラフト後に残るアイテム」も一緒に出す
        for (int i = 0; i < GRID_SIZE; i++) {
            final ItemStack remain = ForgeHooks.getContainerItem(this.grid.getStackInSlot(i));
            if (!remain.isEmpty()) {
                this.pending.add(AEItemStack.fromItemStack(remain));
            }
            this.grid.setStackInSlot(i, ItemStack.EMPTY);
        }

        AppliedManaita.LOGGER.debug("[組立機 {}] クラフト完了 ({}): {}", this.pos, forced ? "ME" : "単体", this.pending);
        if (forced) {
            this.clearJob();
        }
        this.flushPending();
    }

    /** 処理を中止し、持っている素材を出力待ちへ移す(CPUの仕事ならネットワークへ返る) */
    private void abortJob() {
        this.pendingToNetwork = !this.jobPattern.isEmpty();
        for (int i = 0; i < GRID_SIZE; i++) {
            final ItemStack stack = this.grid.getStackInSlot(i);
            if (!stack.isEmpty()) {
                this.pending.add(AEItemStack.fromItemStack(stack));
                this.grid.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        this.clearJob();
        this.flushPending();
    }

    private void clearJob() {
        this.job = null;
        this.jobPattern = ItemStack.EMPTY;
        this.jobOutputs.clear();
        this.progress = 0;
        this.saveChanges();
    }

    // ---------------------------------------------------------------- 出力

    private void flushPending() {
        if (this.pending.isEmpty()) {
            return;
        }
        if (this.pendingToNetwork) {
            this.flushToNetwork();
        } else {
            this.flushToNeighbors();
        }
        this.saveChanges();
    }

    /** ME ネットワークへ入れる。入りきらなかった分は残す */
    private void flushToNetwork() {
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
                    AppliedManaita.LOGGER.debug("[組立機 {}] ネットワークに入りきりませんでした: {}", this.pos, left);
                    stack.setStackSize(left.getStackSize());
                }
            }
        } catch (final GridAccessException e) {
            AppliedManaita.LOGGER.debug("[組立機 {}] MEネットワークにつながっていません", this.pos);
        }
    }

    /**
     * 隣のインベントリへ出す(AE2の分子組立機と同じ)。入りきらなかった分は出口に残る。
     * 1tickに出すのは {@link #MAX_STACKS_PER_FLUSH} スタックまで(残りは次のtick)。
     */
    private void flushToNeighbors() {
        if (this.world == null) {
            return;
        }
        int budget = MAX_STACKS_PER_FLUSH;
        for (final EnumFacing side : EnumFacing.VALUES) {
            if (this.pending.isEmpty() || budget <= 0) {
                return;
            }
            final TileEntity te = this.world.getTileEntity(this.pos.offset(side));
            if (te == null || te instanceof TileManaitaAssembler) {
                continue;
            }
            final IItemHandler target = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (target == null) {
                continue;
            }

            final Iterator<IAEItemStack> it = this.pending.iterator();
            while (it.hasNext() && budget > 0) {
                final IAEItemStack stack = it.next();
                // ItemStack の上限ごとに区切って入れる
                while (stack.getStackSize() > 0 && budget > 0) {
                    budget--; // 入らなかった試しも数える(必ず止まるように)
                    final ItemStack chunk = stack.createItemStack();
                    chunk.setCount((int) Math.min(chunk.getMaxStackSize(), stack.getStackSize()));
                    final ItemStack left = ItemHandlerHelper.insertItemStacked(target, chunk, false);
                    final int moved = chunk.getCount() - left.getCount();
                    stack.decStackSize(moved);
                    if (moved == 0) {
                        break; // このインベントリはもう入らない
                    }
                }
                if (stack.getStackSize() <= 0) {
                    it.remove();
                }
            }
        }
    }

    // ---------------------------------------------------------------- 状態変化

    private void onInventoryChanged() {
        if (this.world != null && !this.world.isRemote) {
            this.saveChanges();
            this.wake();
        }
    }

    private void wake() {
        this.reboot = true;
        try {
            this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
        } catch (final GridAccessException e) {
            // ネットワーク未接続
        }
    }

    /** パターン欄のまな板パターン(なければ null) */
    @Nullable
    private ManaitaPatternDetails getPlan() {
        final ItemStack stack = this.patternInv.getStackInSlot(0);
        if (stack.isEmpty()) {
            this.cachedPlanStack = ItemStack.EMPTY;
            this.cachedPlan = null;
            return null;
        }
        if (this.cachedPlan == null || !ItemStack.areItemStacksEqual(stack, this.cachedPlanStack)) {
            this.cachedPlanStack = stack.copy();
            final ICraftingPatternDetails details = ModRegistry.MANAITA_PATTERN.getPatternForItem(stack, this.world);
            this.cachedPlan = details instanceof ManaitaPatternDetails ? (ManaitaPatternDetails) details : null;
        }
        return this.cachedPlan;
    }

    // ---------------------------------------------------------------- AE2のアップグレードできる機械として

    /** 設定項目はないが、IUpgradeableHost に必要なので空のものを持つ */
    private final IConfigManager settings = new ConfigManager((manager, setting, value) -> {
    });

    /**
     * AE2 の「アップグレードできる機械」として認識させる。これで AE2 と同じく、
     * 加速カードを持ってスニーク＋右クリックすると、入るだけまとめて入る(処理は AE2 のカード側)。
     */
    @Override
    public int getInstalledUpgrades(final Upgrades u) {
        return u == Upgrades.SPEED ? this.getInstalledSpeedCards() : 0;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.settings;
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        return "upgrades".equals(name) ? this.upgrades : null;
    }

    // ---------------------------------------------------------------- AE2

    @Override
    @Nonnull
    public AECableType getCableConnectionType(@Nonnull final AEPartLocation dir) {
        return AECableType.COVERED;
    }

    // ---------------------------------------------------------------- 電源の状態(光るモデル用)

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
            this.markForUpdate(); // クライアントへ送って見た目を変える
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

    // ---------------------------------------------------------------- 外部(ホッパーなど)

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.external);
        }
        return super.getCapability(capability, facing);
    }

    /** 外から見たインベントリ: 0〜8 素材(入れるだけ)、9 出口(取り出すだけ) */
    private class ExternalHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return GRID_SIZE + 1;
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(final int slot) {
            return slot < GRID_SIZE ? TileManaitaAssembler.this.grid.getStackInSlot(slot)
                    : TileManaitaAssembler.this.output.getStackInSlot(0);
        }

        @Override
        @Nonnull
        public ItemStack insertItem(final int slot, @Nonnull final ItemStack stack, final boolean simulate) {
            if (slot >= GRID_SIZE || !TileManaitaAssembler.this.canInsertGrid(slot, stack)) {
                return stack;
            }
            return TileManaitaAssembler.this.grid.insertItem(slot, stack, simulate);
        }

        @Override
        @Nonnull
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return slot == GRID_SIZE ? TileManaitaAssembler.this.output.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(final int slot) {
            return slot < GRID_SIZE ? 1 : 64;
        }
    }

    /**
     * 出口欄。中身は「出力待ち」の先頭を、普通のスタックの大きさに区切って見せたもの。
     * 取り出せるのは単体動作のときだけ(CPUの仕事の結果はネットワークへ行く)。
     * クライアント側では、サーバーから送られた見た目だけを持つ。
     */
    public class OutputHandler extends ItemStackHandler {
        private ItemStack clientView = ItemStack.EMPTY;

        OutputHandler() {
            super(1);
        }

        private boolean isClient() {
            return TileManaitaAssembler.this.world != null && TileManaitaAssembler.this.world.isRemote;
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(final int slot) {
            if (this.isClient()) {
                return this.clientView;
            }
            if (TileManaitaAssembler.this.pending.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final IAEItemStack first = TileManaitaAssembler.this.pending.get(0);
            final ItemStack view = first.createItemStack();
            view.setCount((int) Math.min(view.getMaxStackSize(), first.getStackSize()));
            return view;
        }

        @Override
        public void setStackInSlot(final int slot, @Nonnull final ItemStack stack) {
            // サーバーからの同期(クライアント側)だけ受け付ける
            if (this.isClient()) {
                this.clientView = stack;
            }
        }

        @Override
        @Nonnull
        public ItemStack insertItem(final int slot, @Nonnull final ItemStack stack, final boolean simulate) {
            return stack;
        }

        @Override
        public boolean isItemValid(final int slot, @Nonnull final ItemStack stack) {
            return false;
        }

        @Override
        @Nonnull
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            if (this.isClient()) {
                // 画面での予測用
                if (this.clientView.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                final ItemStack taken = this.clientView.copy();
                taken.setCount(Math.min(amount, taken.getCount()));
                if (!simulate) {
                    this.clientView.shrink(taken.getCount());
                }
                return taken;
            }
            if (TileManaitaAssembler.this.pendingToNetwork || TileManaitaAssembler.this.pending.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final IAEItemStack first = TileManaitaAssembler.this.pending.get(0);
            final ItemStack taken = first.createItemStack();
            taken.setCount((int) Math.min(Math.min(amount, taken.getMaxStackSize()), first.getStackSize()));
            if (!simulate) {
                first.decStackSize(taken.getCount());
                if (first.getStackSize() <= 0) {
                    TileManaitaAssembler.this.pending.remove(0);
                }
                TileManaitaAssembler.this.saveChanges();
                TileManaitaAssembler.this.wake();
            }
            return taken;
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 64;
        }
    }

    // ---------------------------------------------------------------- 保存

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);

        if (!this.jobPattern.isEmpty()) {
            data.setTag("jobPattern", this.jobPattern.writeToNBT(new NBTTagCompound()));
        }
        final NBTTagList jobOutputsTag = new NBTTagList();
        for (final IAEItemStack stack : this.jobOutputs) {
            final NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            jobOutputsTag.appendTag(tag);
        }
        data.setTag("jobOutputs", jobOutputsTag);
        data.setTag("gridInv", this.grid.serializeNBT());
        data.setTag("patternInv", this.patternInv.serializeNBT());
        data.setTag("upgradeInv", this.upgrades.serializeNBT());

        final NBTTagList pendingTag = new NBTTagList();
        for (final IAEItemStack stack : this.pending) {
            final NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            pendingTag.appendTag(tag);
        }
        data.setTag("pending", pendingTag);
        data.setBoolean("pendingToNetwork", this.pendingToNetwork);
        data.setInteger("progress", this.progress);
        return data;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);

        this.job = null; // パターンの中身は最初のtickで復元する
        this.jobPattern = data.hasKey("jobPattern") ? new ItemStack(data.getCompoundTag("jobPattern")) : ItemStack.EMPTY;
        this.jobOutputs.clear();
        final NBTTagList jobOutputsTag = data.getTagList("jobOutputs", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < jobOutputsTag.tagCount(); i++) {
            final IAEItemStack stack = AEItemStack.fromNBT(jobOutputsTag.getCompoundTagAt(i));
            if (stack != null) {
                this.jobOutputs.add(stack);
            }
        }

        if (data.hasKey("gridInv")) {
            this.grid.deserializeNBT(data.getCompoundTag("gridInv"));
        } else if (data.hasKey("grid")) {
            // 前のバージョンの保存形式
            final NBTTagList old = data.getTagList("grid", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < GRID_SIZE && i < old.tagCount(); i++) {
                this.grid.setStackInSlot(i, new ItemStack(old.getCompoundTagAt(i)));
            }
        }
        this.patternInv.deserializeNBT(data.getCompoundTag("patternInv"));
        this.upgrades.deserializeNBT(data.getCompoundTag("upgradeInv"));

        this.pending.clear();
        final NBTTagList pendingTag = data.getTagList("pending", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < pendingTag.tagCount(); i++) {
            final IAEItemStack stack = AEItemStack.fromNBT(pendingTag.getCompoundTagAt(i));
            if (stack != null) {
                this.pending.add(stack);
            }
        }
        // 前のバージョンでは出力待ちは必ずネットワーク行きだった
        this.pendingToNetwork = !data.hasKey("pendingToNetwork") || data.getBoolean("pendingToNetwork");
        this.progress = data.getInteger("progress");
    }

    // ---------------------------------------------------------------- 破壊時

    /** 破壊されたときに落とすアイテム(素材・パターン・加速カード・出力待ち) */
    public List<ItemStack> getContentsForDrop() {
        final List<ItemStack> drops = new ArrayList<>();
        for (final ItemStackHandler inv : new ItemStackHandler[]{this.grid, this.patternInv, this.upgrades}) {
            for (int i = 0; i < inv.getSlots(); i++) {
                final ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    drops.add(stack.copy());
                }
            }
        }
        for (final IAEItemStack stack : this.pending) {
            long left = stack.getStackSize();
            final ItemStack base = stack.createItemStack();
            final int max = Math.max(1, base.getMaxStackSize());
            // 大量にあるとアイテムエンティティでサーバーが重くなるので、最大256スタックまで
            for (int n = 0; left > 0 && n < 256; n++) {
                final ItemStack part = base.copy();
                part.setCount((int) Math.min(max, left));
                left -= part.getCount();
                drops.add(part);
            }
            if (left > 0) {
                AppliedManaita.LOGGER.warn("[組立機 {}] 壊したときに出力待ちが多すぎたため、{} を {} 個落とせませんでした",
                        this.pos, base.getDisplayName(), left);
            }
        }
        return drops;
    }
}

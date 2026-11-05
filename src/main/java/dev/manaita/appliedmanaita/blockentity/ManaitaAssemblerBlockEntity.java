package dev.manaita.appliedmanaita.blockentity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.inventories.ItemTransfer;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.menu.AutoCraftingMenu;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;

import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.ModRegistry;
import dev.manaita.appliedmanaita.crafting.ManaitaCraftingPattern;
import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import dev.manaita.appliedmanaita.crafting.PatternUtil;

/**
 * まな板分子組立機。AE2 の分子組立機と同じく、2通りの使い方ができる。
 * <ol>
 *     <li><b>パターンプロバイダーの隣に置く</b>: プロバイダーに入っている「まな板クラフトパターン」の素材を受け取り、
 *     倍率をかけた数のクラフト結果を ME ネットワークに直接入れる(CPU はそれを待っている)。</li>
 *     <li><b>単体で使う</b>: 画面のパターン欄にまな板クラフトパターンを入れ、ホッパーなどで素材を入れると、
 *     クラフトして隣のインベントリへ出す。隣に入らない分は出口に残り、ホッパーや画面から取り出せる。</li>
 * </ol>
 * 加速カード(最大5枚)の効果と電力の割増は AE2 の分子組立機と同じ。
 */
public class ManaitaAssemblerBlockEntity extends AENetworkedInvBlockEntity
        implements IUpgradeableObject, IGridTickable, ICraftingMachine, IPowerChannelState {

    /** 画面から使うインベントリ(0〜8: 素材、9: 出口、10: パターン欄。AE2 の分子組立機と同じ並び) */
    public static final ResourceLocation INV_MAIN = AppliedManaita.id("manaita_assembler");

    public static final int GRID_SIZE = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int UPGRADE_SLOTS = 5;
    public static final int PROGRESS_MAX = 100;

    /** AE2 の分子組立機と同じ: 加速カードの枚数ごとの「1tickの進み」と「電力の割増」 */
    private static final int[] SPEED = {10, 13, 17, 20, 25, 50};
    private static final double[] POWER_TAX = {1.0, 1.3, 1.7, 2.0, 2.5, 5.0};

    /**
     * 単体動作で、1tickに隣のインベントリへ出す回数の上限。
     * 隣が何でも吸い込むブロック(ゴミ箱など)で倍率が極端に大きいと、出し切るまでループしてサーバーが固まるため。
     * 出し切れなかった分は次のtickに回る。
     */
    private static final int MAX_STACKS_PER_FLUSH = 256;

    /** 壊したときに落とすスタック数の上限(アイテムが多すぎてサーバーが止まらないように) */
    private static final int MAX_DROP_STACKS = 1000;

    /** 0〜8: 3x3 の素材(1マス1個まで)、9: 出口 */
    private final AppEngInternalInventory gridInv = new AppEngInternalInventory(this, GRID_SIZE + 1, 1);
    /** 単体で使うときのパターン欄 */
    private final AppEngInternalInventory patternInv = new AppEngInternalInventory(this, 1, 1);
    /** 外(ホッパーなど)から見たインベントリ: 素材は入れるだけ、出口は取り出すだけ */
    private final InternalInventory gridInvExt = new FilteredInternalInventory(this.gridInv, new ExternalFilter());
    private final InternalInventory internalInv = new CombinedInternalInventory(this.gridInv, this.patternInv);
    private final IUpgradeInventory upgrades;
    private final CraftingContainer craftingInv = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
    private final IActionSource source = IActionSource.ofMachine(this);

    /** 出力待ち(出口に入りきらない分も含む)。個数は long */
    private final List<GenericStack> pending = new ArrayList<>();
    /** true: 出力待ちを ME ネットワークへ入れる(CPU の仕事) / false: 隣のインベントリへ出す(単体動作) */
    private boolean pendingToNetwork = false;

    /** パターンプロバイダーから渡された仕事(ワールドを読み込み直した直後は jobPattern だけがある) */
    private ItemStack jobPattern = ItemStack.EMPTY;
    @Nullable
    private ManaitaCraftingPattern job = null;
    /**
     * 素材を受け取った瞬間の出力(倍率込み)。CPU はこの瞬間の数を「返ってくる数」として記録するので、
     * 作っている途中で設定の倍率が変わっても、この数で作れば CPU とずれない。
     */
    private final List<GenericStack> jobOutputs = new ArrayList<>();

    /** パターン欄のパターン(作り直しを減らすためにキャッシュ) */
    private ItemStack cachedPlanStack = ItemStack.EMPTY;
    @Nullable
    private ManaitaCraftingPattern cachedPlan = null;

    private int progress = 0;

    /**
     * 休んでいた状態から起きた直後か。AE2 と同じく、このときは「前回から何tick経ったか」を1として数える。
     * (数えないと、休んでいた時間の分だけ一気に進んで、ゲージがたまる間もなく完成してしまう)
     */
    private boolean reboot = true;

    /** 電源が入っているか(AE2 と同じく、光る見た目の切り替えに使う。クライアントへ同期する) */
    private boolean isPowered = false;

    public ManaitaAssemblerBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.getMainNode()
                .setIdlePowerUsage(0.0)
                .addService(IGridTickable.class, this);
        this.upgrades = UpgradeInventories.forMachine(ModRegistry.MANAITA_ASSEMBLER.get(), UPGRADE_SLOTS, this::saveChanges);
        this.patternInv.setFilter(new PatternFilter());
    }

    // ---------------------------------------------------------------- 画面から使うもの

    public int getCraftingProgress() {
        return this.progress;
    }

    public int getInstalledSpeedCards() {
        return Math.min(this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD), SPEED.length - 1);
    }

    /** 作業まな板の倍率(サーバー側の値) */
    public int getMultiplier() {
        return ManaitaMultiplier.get(ManaitaMultiplier.Kind.CRAFTING, this.isClientSide());
    }

    /** 素材欄に入れてよいか(パターン欄のレシピに合うものだけ。CPU の仕事中は不可) */
    public boolean canInsertGrid(final int slot, final ItemStack stack) {
        if (!this.jobPattern.isEmpty() || stack.isEmpty() || slot < 0 || slot >= GRID_SIZE) {
            return false;
        }
        final ManaitaCraftingPattern plan = this.getPlan();
        return plan != null && this.level != null && plan.isItemValid(slot, AEItemKey.of(stack), this.level);
    }

    /** 素材欄から取り出してよいか(CPU の仕事中は不可) */
    public boolean canExtractGrid() {
        return this.jobPattern.isEmpty();
    }

    /** パターン欄のまな板クラフトパターン(なければ null)。クライアント側でも使える */
    @Nullable
    public ManaitaCraftingPattern getPlan() {
        final ItemStack stack = this.patternInv.getStackInSlot(0);
        if (stack.isEmpty() || this.level == null) {
            this.cachedPlanStack = ItemStack.EMPTY;
            this.cachedPlan = null;
            return null;
        }
        if (this.cachedPlan == null || !ItemStack.isSameItemSameComponents(stack, this.cachedPlanStack)) {
            this.cachedPlanStack = stack.copy();
            this.cachedPlan = PatternDetailsHelper.decodePattern(stack, this.level) instanceof ManaitaCraftingPattern p ? p : null;
        }
        return this.cachedPlan;
    }

    // ---------------------------------------------------------------- ICraftingMachine (パターンプロバイダーから)

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        final Component name = this.hasCustomName() ? this.getCustomName()
                : ModRegistry.MANAITA_ASSEMBLER_ITEM.get().getDescription();
        final AEItemKey icon = AEItemKey.of(ModRegistry.MANAITA_ASSEMBLER_ITEM.get());

        // AE2 と同じく、加速カードの枚数で見分けられるようにする
        final int cards = this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        final List<Component> tooltip = cards == 0 ? List.of() : List.of(GuiText.CompatibleUpgrade.text(
                Tooltips.of(AEItems.SPEED_CARD.asItem().getDescription()),
                Tooltips.ofUnformattedNumber(cards)));
        return new PatternContainerGroup(icon, name, tooltip);
    }

    @Override
    public boolean acceptsPlans() {
        // AE2 と同じく、パターン欄を使っているときはプロバイダーの仕事を受けない
        return this.patternInv.isEmpty()
                && this.jobPattern.isEmpty()
                && this.pending.isEmpty()
                && this.gridInv.isEmpty();
    }

    @Override
    public boolean pushPattern(final IPatternDetails patternDetails, final KeyCounter[] table, final Direction ejectionDirection) {
        if (!this.acceptsPlans() || !(patternDetails instanceof ManaitaCraftingPattern pattern)) {
            return false;
        }

        // CPU から渡された素材を 3x3 に並べる(AE2 の分子組立機と同じ)
        pattern.fillCraftingGrid(table, this.gridInv::setItemDirect);
        // 念のため: 並べきれなかった素材があれば ME ネットワークへ返す(来ないはず)
        for (final KeyCounter list : table) {
            list.removeZeros();
            for (final var entry : list) {
                this.pending.add(new GenericStack(entry.getKey(), entry.getLongValue()));
                this.pendingToNetwork = true;
            }
            list.clear();
        }

        this.job = pattern;
        this.jobPattern = pattern.getDefinition().toStack();
        this.jobOutputs.clear();
        this.jobOutputs.addAll(pattern.getOutputs());
        this.progress = 0;
        this.saveChanges();
        this.wake();
        AppliedManaita.LOGGER.debug("[まな板分子組立機 {}] 素材を受け取りました: {}", this.worldPosition, pattern.getPrimaryOutput());
        return true;
    }

    // ---------------------------------------------------------------- tick

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        // AE2 の分子組立機と同じく毎tick呼んでもらう
        // (間隔を空けると、まとめて何tick分も進んでゲージが飛び、一瞬で完成したように見える)
        return new TickingRequest(1, 1, !this.hasWork());
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, int ticksSinceLastCall) {
        if (this.reboot) {
            ticksSinceLastCall = 1;
            this.reboot = false;
        }

        // 1. 出力待ちを出し切るまでは次を作らない
        if (this.hasPendingOutput()) {
            this.flushPending();
            if (this.hasPendingOutput()) {
                this.reboot = true; // 待っていた時間はクラフトの進みに数えない
                return TickRateModulation.SLOWER; // 入りきらない。少し待って再挑戦
            }
        }

        // 2. 今やる仕事を決める(プロバイダーの仕事が優先)
        final ManaitaCraftingPattern plan;
        final boolean forced = !this.jobPattern.isEmpty();
        if (forced) {
            if (this.job == null) {
                // ワールド読み込み直後は、保存しておいたパターンから中身を作り直す
                if (this.level != null
                        && PatternDetailsHelper.decodePattern(this.jobPattern, this.level) instanceof ManaitaCraftingPattern restored) {
                    this.job = restored;
                } else {
                    AppliedManaita.LOGGER.warn("[まな板分子組立機 {}] 読み込み後にパターンを復元できませんでした: {}",
                            this.worldPosition, this.jobPattern);
                    this.abortJob();
                    return this.sleep();
                }
            }
            plan = this.job;
        } else {
            plan = this.getPlan();
            // パターンを抜いた・差し替えたなどで、レシピに合わない素材が残っていたら出口へ出す(AE2 と同じ)
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

    /** 出口か出力待ちに何か残っているか */
    private boolean hasPendingOutput() {
        return !this.pending.isEmpty() || !this.gridInv.getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    private boolean hasWork() {
        if (!this.pending.isEmpty() || !this.jobPattern.isEmpty()) {
            return true;
        }
        final ManaitaCraftingPattern plan = this.getPlan();
        return this.hasInvalidGridItems(plan) || (plan != null && this.hasMats(plan));
    }

    /** 素材欄に、今のパターンに合わないアイテムがあるか(パターンがないなら、何か入っていれば true) */
    private boolean hasInvalidGridItems(@Nullable final ManaitaCraftingPattern plan) {
        for (int i = 0; i < GRID_SIZE; i++) {
            final ItemStack stack = this.gridInv.getStackInSlot(i);
            if (!stack.isEmpty() && (plan == null || this.level == null
                    || !plan.isItemValid(i, AEItemKey.of(stack), this.level))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 単体動作で、今のパターンに合わない素材を出力待ちへ移す(隣のインベントリか出口へ出る)。
     * これをしないと素材欄が埋まったままになり、プロバイダーの仕事も受けられなくなる。
     *
     * @return 何か移したら true
     */
    private boolean ejectInvalidGridItems(@Nullable final ManaitaCraftingPattern plan) {
        boolean moved = false;
        for (int i = 0; i < GRID_SIZE; i++) {
            final ItemStack stack = this.gridInv.getStackInSlot(i);
            if (!stack.isEmpty() && (plan == null || this.level == null
                    || !plan.isItemValid(i, AEItemKey.of(stack), this.level))) {
                this.addPending(stack);
                this.gridInv.setItemDirect(i, ItemStack.EMPTY);
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
        final var grid = this.getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        final double used = grid.getEnergyService()
                .extractAEPower(ticks * bonus * tax, Actionable.MODULATE, PowerMultiplier.CONFIG);
        return (int) (used / tax);
    }

    private boolean hasMats(final ManaitaCraftingPattern plan) {
        if (this.level == null) {
            return false;
        }
        for (int x = 0; x < GRID_SIZE; x++) {
            this.craftingInv.setItem(x, this.gridInv.getStackInSlot(x));
        }
        return !plan.assemble(this.craftingInv.asCraftInput(), this.level).isEmpty();
    }

    private void craft(final ManaitaCraftingPattern plan, final boolean forced) {
        if (this.level == null) {
            return;
        }
        for (int x = 0; x < GRID_SIZE; x++) {
            this.craftingInv.setItem(x, this.gridInv.getStackInSlot(x));
        }
        final CraftingInput input = this.craftingInv.asPositionedCraftInput().input();

        final ItemStack check = plan.assemble(input, this.level);
        if (check.isEmpty()) {
            // 素材がレシピに合わない。素材は返す
            AppliedManaita.LOGGER.debug("[まな板分子組立機 {}] 素材がレシピに合わないので中止しました", this.worldPosition);
            this.abortJob();
            return;
        }

        this.pendingToNetwork = forced;

        // 出力(倍率込み)。個数は long のまま。
        // CPU の仕事は受け取った瞬間の数で、単体動作は今の倍率で作る
        final List<GenericStack> outputs = forced && !this.jobOutputs.isEmpty() ? this.jobOutputs : plan.getOutputs();
        for (final GenericStack out : outputs) {
            this.addPending(out);
        }

        // バケツなどの「クラフト後に残るアイテム」も一緒に出す(CPU はこれも待っている)
        for (final ItemStack remain : plan.getRemainingItems(input)) {
            if (!remain.isEmpty()) {
                this.addPending(remain);
            }
        }
        for (int x = 0; x < GRID_SIZE; x++) {
            this.gridInv.setItemDirect(x, ItemStack.EMPTY);
        }

        AppliedManaita.LOGGER.debug("[まな板分子組立機 {}] クラフト完了 ({}): {}", this.worldPosition, forced ? "ME" : "単体", this.pending);
        if (forced) {
            this.clearJob();
        }
        this.flushPending();
    }

    /** 処理を中止し、持っている素材を出力待ちへ移す(CPU の仕事ならネットワークへ返る) */
    private void abortJob() {
        this.pendingToNetwork = !this.jobPattern.isEmpty();
        for (int i = 0; i < GRID_SIZE; i++) {
            final ItemStack stack = this.gridInv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                this.addPending(stack);
                this.gridInv.setItemDirect(i, ItemStack.EMPTY);
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

    private void addPending(final ItemStack stack) {
        final AEItemKey key = AEItemKey.of(stack);
        if (key != null) {
            this.addPending(new GenericStack(key, stack.getCount()));
        }
    }

    private void addPending(final GenericStack stack) {
        if (stack.amount() <= 0) {
            return;
        }
        for (int i = 0; i < this.pending.size(); i++) {
            final GenericStack p = this.pending.get(i);
            if (p.what().equals(stack.what())) {
                this.pending.set(i, new GenericStack(p.what(), saturatedAdd(p.amount(), stack.amount())));
                return;
            }
        }
        this.pending.add(stack);
    }

    private static long saturatedAdd(final long a, final long b) {
        final long sum = a + b;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    private void flushPending() {
        if (!this.hasPendingOutput()) {
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
        final var grid = this.getMainNode().getGrid();
        if (grid == null) {
            AppliedManaita.LOGGER.debug("[まな板分子組立機 {}] ME ネットワークにつながっていません", this.worldPosition);
            return;
        }
        final var storage = grid.getStorageService().getInventory();
        for (int i = 0; i < this.pending.size(); ) {
            final GenericStack stack = this.pending.get(i);
            final long inserted = storage.insert(stack.what(), stack.amount(), Actionable.MODULATE, this.source);
            final long left = stack.amount() - inserted;
            if (left <= 0) {
                this.pending.remove(i);
            } else {
                AppliedManaita.LOGGER.debug("[まな板分子組立機 {}] ネットワークに入りきりませんでした: {} x {}",
                        this.worldPosition, stack.what(), left);
                this.pending.set(i, new GenericStack(stack.what(), left));
                i++;
            }
        }
        // 念のため: 出口に残っているもの(前の単体動作の分)もネットワークへ
        final ItemStack out = this.gridInv.getStackInSlot(OUTPUT_SLOT);
        if (!out.isEmpty()) {
            final AEItemKey key = AEItemKey.of(out);
            final long inserted = key == null ? 0 : storage.insert(key, out.getCount(), Actionable.MODULATE, this.source);
            final ItemStack left = out.copy();
            left.shrink((int) inserted);
            this.gridInv.setItemDirect(OUTPUT_SLOT, left);
        }
    }

    /**
     * 隣のインベントリへ出す(AE2 の分子組立機と同じ)。入りきらなかった分は出口に残る。
     * 1tickに出すのは {@link #MAX_STACKS_PER_FLUSH} 回まで(残りは次のtick)。
     */
    private void flushToNeighbors() {
        if (this.level == null) {
            return;
        }
        for (int budget = MAX_STACKS_PER_FLUSH; budget > 0; budget--) {
            this.refillOutputSlot();
            final ItemStack out = this.gridInv.getStackInSlot(OUTPUT_SLOT);
            if (out.isEmpty()) {
                return;
            }
            final int before = out.getCount();
            final ItemStack left = this.pushToNeighbors(out.copy());
            this.gridInv.setItemDirect(OUTPUT_SLOT, left);
            if (!left.isEmpty() && left.getCount() == before) {
                return; // どこにも入らない。出口に残して、ホッパーや画面から取り出してもらう
            }
        }
    }

    /** 出力待ちの先頭を、出口に普通のスタックの大きさまで移す */
    private void refillOutputSlot() {
        if (this.pending.isEmpty()) {
            return;
        }
        final GenericStack first = this.pending.get(0);
        if (!(first.what() instanceof AEItemKey key)) {
            this.pending.remove(0); // アイテム以外は単体動作では出せない(来ないはず)
            return;
        }
        final ItemStack current = this.gridInv.getStackInSlot(OUTPUT_SLOT);
        final int max = key.toStack().getMaxStackSize();
        final int room;
        if (current.isEmpty()) {
            room = max;
        } else if (key.matches(current)) {
            room = max - current.getCount();
        } else {
            return;
        }
        if (room <= 0) {
            return;
        }
        final int moved = (int) Math.min(room, first.amount());
        this.gridInv.setItemDirect(OUTPUT_SLOT, key.toStack(current.getCount() + moved));
        if (first.amount() - moved <= 0) {
            this.pending.remove(0);
        } else {
            this.pending.set(0, new GenericStack(key, first.amount() - moved));
        }
    }

    private ItemStack pushToNeighbors(ItemStack output) {
        for (final Direction d : Direction.values()) {
            if (output.isEmpty()) {
                break;
            }
            final BlockPos target = this.worldPosition.relative(d);
            final BlockEntity be = this.level.getBlockEntity(target);
            if (be instanceof ManaitaAssemblerBlockEntity) {
                continue; // まな板分子組立機どうしで渡し合わない
            }
            final ItemTransfer adaptor = InternalInventory.wrapExternal(this.level, target, d.getOpposite());
            if (adaptor != null) {
                output = adaptor.addItems(output);
            }
        }
        return output;
    }

    // ---------------------------------------------------------------- 状態変化

    @Override
    public void onChangeInventory(final AppEngInternalInventory inv, final int slot) {
        if (inv == this.patternInv) {
            this.progress = 0;
        }
        if (this.level != null && !this.level.isClientSide()) {
            this.wake();
        }
    }

    private void wake() {
        this.reboot = true;
        this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    // ---------------------------------------------------------------- 電源の状態(光る見た目用)

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
                this.markForUpdate(); // クライアントへ送って見た目を変える
            }
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
    protected boolean readFromStream(final RegistryFriendlyByteBuf data) {
        final boolean changed = super.readFromStream(data);
        final boolean old = this.isPowered;
        this.isPowered = data.readBoolean();
        return changed || old != this.isPowered;
    }

    @Override
    protected void writeToStream(final RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.isPowered);
    }

    // ---------------------------------------------------------------- インベントリ

    @Override
    public AECableType getCableConnectionType(final Direction dir) {
        return AECableType.COVERED;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.internalInv;
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(final Direction side) {
        return this.gridInvExt;
    }

    @Override
    public InternalInventory getSubInventory(final ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        } else if (id.equals(INV_MAIN)) {
            return this.internalInv;
        }
        return super.getSubInventory(id);
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    // ---------------------------------------------------------------- 保存

    @Override
    public void saveAdditional(final CompoundTag data, final HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.upgrades.writeToNBT(data, "upgrades", registries);
        if (!this.jobPattern.isEmpty()) {
            data.put("jobPattern", this.jobPattern.save(registries));
        }
        data.put("jobOutputs", writeStacks(this.jobOutputs, registries));
        data.put("pending", writeStacks(this.pending, registries));
        data.putBoolean("pendingToNetwork", this.pendingToNetwork);
        data.putInt("progress", this.progress);
    }

    @Override
    public void loadTag(final CompoundTag data, final HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.upgrades.readFromNBT(data, "upgrades", registries);
        this.job = null; // パターンの中身は最初のtickで作り直す
        this.jobPattern = data.contains("jobPattern")
                ? ItemStack.parseOptional(registries, data.getCompound("jobPattern"))
                : ItemStack.EMPTY;
        readStacks(data.getList("jobOutputs", Tag.TAG_COMPOUND), registries, this.jobOutputs);
        readStacks(data.getList("pending", Tag.TAG_COMPOUND), registries, this.pending);
        this.pendingToNetwork = data.getBoolean("pendingToNetwork");
        this.progress = data.getInt("progress");
        this.reboot = true;
    }

    private static ListTag writeStacks(final List<GenericStack> stacks, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final GenericStack stack : stacks) {
            list.add(GenericStack.writeTag(registries, stack));
        }
        return list;
    }

    private static void readStacks(final ListTag list, final HolderLookup.Provider registries, final List<GenericStack> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            final GenericStack stack = GenericStack.readTag(registries, list.getCompound(i));
            if (stack != null) {
                into.add(stack);
            }
        }
    }

    // ---------------------------------------------------------------- 破壊時

    @Override
    public void addAdditionalDrops(final Level level, final BlockPos pos, final List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops); // 素材・出口・パターン欄
        for (final ItemStack upgrade : this.upgrades) {
            drops.add(upgrade);
        }
        // 出力待ち(極端な数でワールドが止まらないよう上限を設ける)
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
                AppliedManaita.LOGGER.warn("[まな板分子組立機 {}] 壊したときに出力待ちが多すぎたため、{} を {} 個落とせませんでした",
                        pos, key, left);
            }
        }
        this.pending.clear();
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.upgrades.clear();
        this.pending.clear();
    }

    // ---------------------------------------------------------------- フィルター

    /** パターン欄に入るのは まな板クラフトパターン だけ */
    private static class PatternFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(final InternalInventory inv, final int slot, final ItemStack stack) {
            return PatternUtil.isManaitaCraftingPattern(stack);
        }
    }

    /** 外(ホッパーなど)から: 素材欄には今のパターンに合うものだけ入れられる。取り出せるのは出口だけ */
    private class ExternalFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(final InternalInventory inv, final int slot, final int amount) {
            return slot == OUTPUT_SLOT;
        }

        @Override
        public boolean allowInsert(final InternalInventory inv, final int slot, final ItemStack stack) {
            return slot < GRID_SIZE && ManaitaAssemblerBlockEntity.this.canInsertGrid(slot, stack);
        }
    }
}

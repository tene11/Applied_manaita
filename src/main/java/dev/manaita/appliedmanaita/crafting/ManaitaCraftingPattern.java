package dev.manaita.appliedmanaita.crafting;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.core.localization.GuiText;
import appeng.crafting.pattern.AECraftingPattern;

/**
 * まな板クラフトパターンの中身(作業台レシピ)。まな板分子組立機 が受け取る。
 * <p>
 * 中身は AE2 のクラフトパターンと同じなので、代替素材・液体の代用・クラフト後に残るアイテムなども AE2 と同じに効く。
 * 出力はまな板mod の作業まな板の倍率(crafting_doubling_value)倍。
 * <p>
 * AE2 の {@link IMolecularAssemblerSupportedPattern} は付けていないので、普通の分子組立機は受け取らない
 * (クラフト用の処理は同じ名前のメソッドで持っていて、まな板分子組立機だけが使う)。
 */
public final class ManaitaCraftingPattern implements IManaitaPattern {

    private final AECraftingPattern base;
    private final boolean clientSide;

    /** 今の倍率で計算した出力(倍率が変わったら作り直す) */
    private int multiplier = -1;
    private List<GenericStack> outputs = List.of();

    /**
     * @throws RuntimeException パターンが壊れている、またはレシピに合わなくなった場合(AE2 が投げる)
     */
    public ManaitaCraftingPattern(final AEItemKey definition, final Level level) {
        this.base = new AECraftingPattern(definition, level);
        this.clientSide = level.isClientSide();
        this.refresh();
    }

    private synchronized void refresh() {
        final int now = ManaitaMultiplier.get(ManaitaMultiplier.Kind.CRAFTING, this.clientSide);
        if (now != this.multiplier) {
            this.multiplier = now;
            this.outputs = IManaitaPattern.multiply(this.base.getOutputs(), now);
        }
    }

    @Override
    public ManaitaMultiplier.Kind getKind() {
        return ManaitaMultiplier.Kind.CRAFTING;
    }

    @Override
    public synchronized int getMultiplier() {
        this.refresh();
        return this.multiplier;
    }

    @Override
    public AECraftingPattern getBase() {
        return this.base;
    }

    @Override
    public AEItemKey getDefinition() {
        return this.base.getDefinition();
    }

    @Override
    public IInput[] getInputs() {
        return this.base.getInputs();
    }

    @Override
    public synchronized List<GenericStack> getOutputs() {
        this.refresh();
        return this.outputs;
    }

    @Override
    public PatternDetailsTooltip getTooltip(final Level level, final TooltipFlag flags) {
        // AE2 のクラフトパターンと同じ表示。個数は倍率込みで、最後に倍率の行を足す
        final PatternDetailsTooltip tooltip = new PatternDetailsTooltip(PatternDetailsTooltip.OUTPUT_TEXT_CRAFTS);
        tooltip.addInputsAndOutputs(this);
        if (this.base.canSubstitute()) {
            tooltip.addProperty(GuiText.PatternTooltipSubstitutions.text());
        }
        if (this.base.canSubstituteFluids()) {
            tooltip.addProperty(GuiText.PatternTooltipFluidSubstitutions.text());
        }
        tooltip.addProperty(Component.translatable(this.getKind().tooltipKey(), this.getMultiplier())
                .withStyle(ChatFormatting.GOLD));
        return tooltip;
    }

    // ---------------------------------------------------------------- まな板分子組立機が使うクラフトの処理(AE2 のものをそのまま使う)

    /** この素材をこのマスに入れてよいか */
    public boolean isItemValid(final int slot, final AEItemKey key, final Level level) {
        return this.base.isItemValid(slot, key, level);
    }

    /** このマスを使うか */
    public boolean isSlotEnabled(final int slot) {
        return this.base.isSlotEnabled(slot);
    }

    /** CPU から渡された素材を 3x3 に並べる */
    public void fillCraftingGrid(final KeyCounter[] table, final IMolecularAssemblerSupportedPattern.CraftingGridAccessor gridAccessor) {
        this.base.fillCraftingGrid(table, gridAccessor);
    }

    /** 素材から作れる完成品(倍率をかける前)。素材がレシピに合わなければ空 */
    public ItemStack assemble(final CraftingInput input, final Level level) {
        return this.base.assemble(input, level);
    }

    /** クラフト後に残るアイテム(バケツなど) */
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        return this.base.getRemainingItems(input);
    }

    // ---------------------------------------------------------------- AE2 の決まり: 同じパターンは同じとみなす

    @Override
    public int hashCode() {
        return this.getDefinition().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ManaitaCraftingPattern other && other.getDefinition().equals(this.getDefinition());
    }
}

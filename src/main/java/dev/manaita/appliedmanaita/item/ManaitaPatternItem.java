package dev.manaita.appliedmanaita.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.localization.GuiText;
import appeng.crafting.pattern.EncodedPatternItem;

import dev.manaita.appliedmanaita.crafting.IManaitaPattern;
import dev.manaita.appliedmanaita.crafting.ManaitaCraftingPattern;

/**
 * まな板パターンのアイテム(クラフト用・加工用)に共通するもの。
 * <p>
 * AE2 のエンコード済みパターンのアイテム({@link EncodedPatternItem})を元にしているので、
 * AE2 のパターンプロバイダー・CPU・ターミナルは、AE2 のパターンと同じようにこのアイテムを扱える
 * (シフトで完成品のアイコンになる、シフト＋右クリックで空のパターンに戻る、など)。
 * 説明欄は AE2 と同じ並びで、個数は倍率込み。最後に倍率の行を足す。
 */
public abstract class ManaitaPatternItem extends EncodedPatternItem {

    protected ManaitaPatternItem(final Properties properties) {
        super(properties);
    }

    /** 中身を読み取る。壊れている・レシピに合わないときは null */
    @Nullable
    protected abstract IManaitaPattern create(AEItemKey what, Level level);

    @Nullable
    @Override
    public IManaitaPattern decode(final ItemStack stack, final Level level, final boolean tryRecovery) {
        if (stack.getItem() != this || !stack.hasTag() || level == null) {
            return null;
        }
        return this.decode(AEItemKey.of(stack), level);
    }

    @Nullable
    @Override
    public IManaitaPattern decode(final AEItemKey what, final Level level) {
        if (what == null || !what.hasTag() || level == null || what.getItem() != this) {
            return null;
        }
        try {
            return this.create(what, level);
        } catch (final Exception e) {
            return null;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> lines,
                                final TooltipFlag flags) {
        if (!stack.hasTag()) {
            return;
        }
        final IManaitaPattern details = level == null ? null : this.decode(stack, level, false);
        if (details == null) {
            // 壊れている・レシピに合わない: AE2 と同じ「無効なパターン」の表示
            super.appendHoverText(stack, level, lines, flags);
            return;
        }
        if (stack.hasCustomHoverName()) {
            stack.resetHoverName();
        }

        final boolean crafting = details instanceof ManaitaCraftingPattern;
        final Component label = (crafting ? GuiText.Crafts.text() : GuiText.Produces.text()).copy()
                .append(": ").withStyle(ChatFormatting.GRAY);
        final Component and = Component.literal(" ").append(GuiText.And.text())
                .append(" ").withStyle(ChatFormatting.GRAY);
        final Component with = GuiText.With.text().copy().append(": ").withStyle(ChatFormatting.GRAY);

        boolean first = true;
        for (final GenericStack out : details.getOutputs()) {
            if (out != null) {
                lines.add(Component.empty().append(first ? label : and).append(getStackComponent(out)));
                first = false;
            }
        }
        first = true;
        for (final IPatternDetails.IInput in : details.getInputs()) {
            if (in != null) {
                final GenericStack template = in.getPossibleInputs()[0];
                final GenericStack total = new GenericStack(template.what(), template.amount() * in.getMultiplier());
                lines.add(Component.empty().append(first ? with : and).append(getStackComponent(total)));
                first = false;
            }
        }
        if (details instanceof ManaitaCraftingPattern craftingPattern) {
            lines.add(GuiText.Substitute.text().copy().append(" ")
                    .append(craftingPattern.canSubstitute() ? GuiText.Yes.text() : GuiText.No.text()));
        }
        lines.add(Component.translatable(details.getKind().tooltipKey(), details.getMultiplier())
                .withStyle(ChatFormatting.GOLD));
    }
}

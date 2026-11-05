package dev.manaita.appliedmanaita.crafting;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedProcessingPattern;

import dev.manaita.appliedmanaita.ModRegistry;

/**
 * パターンまわりの共通処理。
 * <p>
 * AE2 と同じく、まな板パターンもクラフト用({@link ModRegistry#MANAITA_CRAFTING_PATTERN})と
 * 加工用({@link ModRegistry#MANAITA_PROCESSING_PATTERN})でアイテムが分かれる。加工用は、中身のレシピで
 * かまど用と醸造用に分かれる。
 */
public final class PatternUtil {

    /** 醸造台と同じく、1回に入れられる元のポーションは3本まで */
    public static final int MAX_BOTTLES = 3;

    private PatternUtil() {
    }

    /** まな板クラフトパターンのアイテムか */
    public static boolean isManaitaCraftingPattern(final ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModRegistry.MANAITA_CRAFTING_PATTERN.get());
    }

    /** まな板加工パターンのアイテムか */
    public static boolean isManaitaProcessingPattern(final ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModRegistry.MANAITA_PROCESSING_PATTERN.get());
    }

    /**
     * 加工パターンが「素材1種類 → そのかまどレシピの完成品」になっているか。
     * 個数も「素材の数 × レシピ1回分の完成品の数」と一致している必要がある
     * (一致しないパターンを認めると、少ない素材で多くの完成品が出てしまうため)。
     */
    public static boolean isSmeltingPattern(final AEProcessingPattern pattern, final Level level) {
        final IPatternDetails.IInput[] inputs = pattern.getInputs();
        final List<GenericStack> outputs = pattern.getOutputs();
        if (inputs.length != 1 || outputs.size() != 1) {
            return false;
        }
        if (!(inputs[0].getPossibleInputs()[0].what() instanceof AEItemKey in)
                || !(outputs.get(0).what() instanceof AEItemKey out)) {
            return false;
        }
        final long inAmount = inputs[0].getMultiplier() * inputs[0].getPossibleInputs()[0].amount();
        final ItemStack result = smelt(in, level);
        if (result.isEmpty() || !out.equals(AEItemKey.of(result))) {
            return false;
        }
        return outputs.get(0).amount() == inAmount * result.getCount();
    }

    /**
     * 加工パターンが「材料1個 ＋ 元のポーション1〜3本 → 醸造の完成品(元のポーションと同じ本数)」になっているか。
     * まな板分子醸造機で使えるのはこの形だけ(個数が比率どおりでないと、少ない素材で多くの完成品が出てしまうため)。
     */
    public static boolean isBrewingPattern(final AEProcessingPattern pattern, final Level level) {
        final IPatternDetails.IInput[] inputs = pattern.getInputs();
        final List<GenericStack> outputs = pattern.getOutputs();
        if (inputs.length != 2 || outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey out)) {
            return false;
        }
        for (int k = 0; k < 2; k++) {
            if (!(inputs[k].getPossibleInputs()[0].what() instanceof AEItemKey ingredient)
                    || !(inputs[1 - k].getPossibleInputs()[0].what() instanceof AEItemKey potion)) {
                return false;
            }
            final long ingredientAmount = inputs[k].getMultiplier() * inputs[k].getPossibleInputs()[0].amount();
            final long bottles = inputs[1 - k].getMultiplier() * inputs[1 - k].getPossibleInputs()[0].amount();
            if (ingredientAmount != 1 || bottles < 1 || bottles > MAX_BOTTLES) {
                continue;
            }
            final ItemStack result = brew(potion, ingredient, level);
            if (!result.isEmpty() && out.equals(AEItemKey.of(result))
                    && outputs.get(0).amount() == bottles * result.getCount()) {
                return true;
            }
        }
        return false;
    }

    /**
     * まな板パターンターミナルで作った加工パターンが醸造レシピなら、まな板加工パターン(醸造用)の形
     * (材料1個 ＋ 元のポーションn本 → 完成品n本分)に直した中身を返す。醸造レシピでなければ null。
     * <p>
     * レシピ表示mod から転送すると「元のポーション3本 ＋ 材料(＋燃料のブレイズパウダー) → 完成品1本」のようになることがあるので、
     * 燃料を外し、完成品の数を元のポーションの本数に合わせる。
     */
    @Nullable
    public static EncodedProcessingPattern toBrewingPattern(final AEProcessingPattern pattern, final Level level) {
        final List<GenericStack> outputs = pattern.getOutputs();
        final List<GenericStack> inputs = condensedInputs(pattern);
        if (outputs.size() != 1 || !(outputs.get(0).what() instanceof AEItemKey out)
                || inputs.size() < 2 || inputs.size() > 3) {
            return null;
        }
        for (final GenericStack potion : inputs) {
            if (!(potion.what() instanceof AEItemKey potionKey) || potion.amount() < 1 || potion.amount() > MAX_BOTTLES) {
                continue;
            }
            for (final GenericStack ingredient : inputs) {
                if (ingredient == potion || !(ingredient.what() instanceof AEItemKey ingredientKey)) {
                    continue;
                }
                final ItemStack result = brew(potionKey, ingredientKey, level);
                if (result.isEmpty() || !out.equals(AEItemKey.of(result))) {
                    continue;
                }
                // 材料は1個。ただし材料がブレイズパウダーなら、燃料の分が混ざって多くてもよい
                if (ingredient.amount() != 1 && !isBlazePowder(ingredientKey)) {
                    continue;
                }
                // 3種類目があってよいのは燃料のブレイズパウダーだけ
                boolean onlyFuelLeft = true;
                for (final GenericStack other : inputs) {
                    if (other != potion && other != ingredient
                            && !(other.what() instanceof AEItemKey otherKey && isBlazePowder(otherKey))) {
                        onlyFuelLeft = false;
                    }
                }
                if (!onlyFuelLeft) {
                    continue;
                }

                final long bottles = potion.amount();
                final List<GenericStack> newInputs = new ArrayList<>();
                newInputs.add(new GenericStack(ingredientKey, 1));
                newInputs.add(new GenericStack(potionKey, bottles));
                final List<GenericStack> newOutputs = new ArrayList<>();
                newOutputs.add(new GenericStack(out, bottles * result.getCount()));
                return new EncodedProcessingPattern(newInputs, newOutputs);
            }
        }
        return null;
    }

    /** 素材を種類ごとにまとめたもの(AE2 の中身と同じ順番) */
    private static List<GenericStack> condensedInputs(final AEProcessingPattern pattern) {
        final List<GenericStack> list = new ArrayList<>();
        for (final IPatternDetails.IInput input : pattern.getInputs()) {
            final GenericStack template = input.getPossibleInputs()[0];
            list.add(new GenericStack(template.what(), template.amount() * input.getMultiplier()));
        }
        return list;
    }

    /** かまどで1個焼いたときの完成品。焼けなければ空 */
    public static ItemStack smelt(final AEItemKey input, final Level level) {
        final SingleRecipeInput recipeInput = new SingleRecipeInput(input.toStack());
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, recipeInput, level)
                .map(holder -> holder.value().assemble(recipeInput, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /** 元のポーション1本と材料1個で醸造したときの完成品(1本分)。醸造できなければ空 */
    public static ItemStack brew(final AEItemKey potion, final AEItemKey ingredient, final Level level) {
        final PotionBrewing brewing = level.potionBrewing();
        final ItemStack input = potion.toStack();
        final ItemStack reagent = ingredient.toStack();
        if (!brewing.hasMix(input, reagent)) {
            return ItemStack.EMPTY;
        }
        final ItemStack result = brewing.mix(reagent, input);
        if (result.isEmpty() || ItemStack.isSameItemSameComponents(result, input)) {
            return ItemStack.EMPTY;
        }
        return result;
    }

    private static boolean isBlazePowder(final AEItemKey key) {
        return key.is(Items.BLAZE_POWDER);
    }
}

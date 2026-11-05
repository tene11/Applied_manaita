package dev.manaita.appliedmanaita.crafting;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import dev.manaita.appliedmanaita.ModRegistry;
import dev.manaita.appliedmanaita.item.ItemManaitaPattern;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static appeng.helpers.ItemStackHelper.stackWriteToNBT;

/**
 * パターンまわりの共通処理。
 * <p>
 * まな板パターンは AE2 と同じく1種類のアイテムで、中の印(NBT の {@code crafting})で
 * 「クラフト用」(true)と「加工用」(false)に分かれる。加工用は、中身のレシピで かまど用 と 醸造用 に分かれる。
 */
public final class PatternUtil {

    /** 醸造台と同じく、1回に入れられる元のポーションは3本まで */
    public static final int MAX_BOTTLES = 3;

    private PatternUtil() {
    }

    /** まな板パターンのアイテムか(クラフト用・かまど用・醸造用を問わない) */
    public static boolean isManaitaPatternItem(final ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModRegistry.MANAITA_PATTERN;
    }

    /**
     * クラフト用のまな板パターンか。まな板分子組立機のパターン欄に入るのはこれだけ。
     */
    public static boolean isManaitaPattern(final ItemStack stack) {
        return isManaitaPatternItem(stack) && ItemManaitaPattern.isCraftingType(stack);
    }

    /**
     * 加工用(かまど用・醸造用)のまな板パターンか(中身があって {@code crafting=false} のもの)。
     */
    public static boolean isManaitaProcessingPattern(final ItemStack stack) {
        return isManaitaPatternItem(stack) && stack.hasTagCompound() && !ItemManaitaPattern.isCraftingType(stack);
    }

    /**
     * 加工パターンが「素材1種類 → そのかまどレシピの完成品」になっているか。
     * 個数も「素材の数 × レシピ1回分の完成品の数」と一致している必要がある
     * (一致しないパターンを認めると、少ない素材で多くの完成品が出てしまうため)。
     */
    public static boolean isSmeltingPattern(final ICraftingPatternDetails details) {
        if (details.isCraftable()) {
            return false;
        }
        final IAEItemStack in = single(details.getCondensedInputs());
        final IAEItemStack out = single(details.getCondensedOutputs());
        if (in == null || out == null) {
            return false;
        }
        final ItemStack one = in.createItemStack();
        one.setCount(1);
        final ItemStack result = FurnaceRecipes.instance().getSmeltingResult(one);
        if (result.isEmpty() || !out.isSameType(result)) {
            return false;
        }
        return out.getStackSize() == in.getStackSize() * result.getCount();
    }

    /**
     * 加工パターンが「材料1個 ＋ 元のポーション1〜3本 → 醸造の完成品(元のポーションと同じ本数)」になっているか。
     * まな板分子醸造機で使えるのはこの形だけ(個数が比率どおりでないと、少ない素材で多くの完成品が出てしまうため)。
     */
    public static boolean isBrewingPattern(final ICraftingPatternDetails details) {
        if (details.isCraftable()) {
            return false;
        }
        final List<IAEItemStack> in = nonNull(details.getCondensedInputs());
        final IAEItemStack out = single(details.getCondensedOutputs());
        if (in.size() != 2 || out == null) {
            return false;
        }
        for (int k = 0; k < 2; k++) {
            final IAEItemStack ingredient = in.get(k);
            final IAEItemStack potion = in.get(1 - k);
            if (ingredient.getStackSize() != 1 || potion.getStackSize() < 1 || potion.getStackSize() > MAX_BOTTLES) {
                continue;
            }
            final ItemStack result = brew(potion, ingredient);
            if (!result.isEmpty() && out.isSameType(result)
                    && out.getStackSize() == potion.getStackSize() * result.getCount()) {
                return true;
            }
        }
        return false;
    }

    /**
     * まな板パターンターミナルで作った加工パターンが醸造レシピなら、醸造用のまな板パターンの形
     * (材料1個 ＋ 元のポーションn本 → 完成品n本分)に直した中身(NBT)を返す。醸造レシピでなければ null。
     * <p>
     * JEI から転送すると「元のポーション3本 ＋ 材料(＋燃料のブレイズパウダー) → 完成品1本」になるので、
     * 燃料を外し、完成品の数を元のポーションの本数に合わせる。
     */
    @Nullable
    public static NBTTagCompound toBrewingPatternTag(final ICraftingPatternDetails details) {
        if (details.isCraftable()) {
            return null;
        }
        final IAEItemStack out = single(details.getCondensedOutputs());
        final List<IAEItemStack> in = nonNull(details.getCondensedInputs());
        if (out == null || in.size() < 2 || in.size() > 3) {
            return null;
        }
        for (final IAEItemStack potion : in) {
            if (potion.getStackSize() < 1 || potion.getStackSize() > MAX_BOTTLES) {
                continue;
            }
            for (final IAEItemStack ingredient : in) {
                if (ingredient == potion) {
                    continue;
                }
                final ItemStack result = brew(potion, ingredient);
                if (result.isEmpty() || !out.isSameType(result)) {
                    continue;
                }
                // 材料は1個。ただし材料がブレイズパウダーなら、燃料の分が混ざって多くてもよい
                if (ingredient.getStackSize() != 1 && !isBlazePowder(ingredient)) {
                    continue;
                }
                // 3種類目があってよいのは燃料のブレイズパウダーだけ
                boolean onlyFuelLeft = true;
                for (final IAEItemStack other : in) {
                    if (other != potion && other != ingredient && !isBlazePowder(other)) {
                        onlyFuelLeft = false;
                    }
                }
                if (!onlyFuelLeft) {
                    continue;
                }

                final int bottles = (int) potion.getStackSize();
                final ItemStack ingredientOne = ingredient.createItemStack();
                ingredientOne.setCount(1);
                final ItemStack potions = potion.createItemStack();
                potions.setCount(bottles);
                final ItemStack products = result.copy();
                products.setCount(bottles * result.getCount());
                return processingTag(new ItemStack[]{ingredientOne, potions}, products);
            }
        }
        return null;
    }

    /** 元のポーション1本と材料1個で醸造したときの完成品(1本分)。醸造できなければ空 */
    private static ItemStack brew(final IAEItemStack potion, final IAEItemStack ingredient) {
        final ItemStack input = potion.createItemStack();
        input.setCount(1);
        final ItemStack reagent = ingredient.createItemStack();
        reagent.setCount(1);
        return BrewingRecipeRegistry.getOutput(input, reagent);
    }

    private static boolean isBlazePowder(final IAEItemStack stack) {
        return stack.getItem() == Items.BLAZE_POWDER;
    }

    /** AE2 の加工パターンと同じ形の中身(NBT) */
    private static NBTTagCompound processingTag(final ItemStack[] inputs, final ItemStack output) {
        final NBTTagList in = new NBTTagList();
        for (final ItemStack stack : inputs) {
            final NBTTagCompound tag = new NBTTagCompound();
            stackWriteToNBT(stack, tag);
            in.appendTag(tag);
        }
        final NBTTagList out = new NBTTagList();
        final NBTTagCompound outTag = new NBTTagCompound();
        stackWriteToNBT(output, outTag);
        out.appendTag(outTag);

        final NBTTagCompound data = new NBTTagCompound();
        data.setTag("in", in);
        data.setTag("out", out);
        data.setBoolean("crafting", false);
        data.setBoolean("substitute", false);
        return data;
    }

    private static List<IAEItemStack> nonNull(final IAEItemStack[] stacks) {
        final List<IAEItemStack> list = new ArrayList<>();
        for (final IAEItemStack s : stacks) {
            if (s != null) {
                list.add(s);
            }
        }
        return list;
    }

    /** null 以外がちょうど1つならそれを返す */
    private static IAEItemStack single(final IAEItemStack[] stacks) {
        IAEItemStack found = null;
        for (final IAEItemStack s : stacks) {
            if (s == null) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = s;
        }
        return found;
    }
}

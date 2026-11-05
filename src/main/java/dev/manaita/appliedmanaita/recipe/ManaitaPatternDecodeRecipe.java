package dev.manaita.appliedmanaita.recipe;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import appeng.api.ids.AEComponents;
import appeng.core.definitions.AEItems;

import dev.manaita.appliedmanaita.ModRegistry;
import dev.manaita.appliedmanaita.crafting.PatternUtil;

/**
 * まな板パターンを作業台に1個だけ置く → 元の AE2 のパターンに戻す。
 * 中身はそのまま写すので、まな板クラフトパターンは AE2 のクラフトパターン、まな板加工パターンは AE2 の加工パターンになる。
 */
public class ManaitaPatternDecodeRecipe extends CustomRecipe {

    public ManaitaPatternDecodeRecipe(final CraftingBookCategory category) {
        super(category);
    }

    /** まな板パターンがちょうど1個だけ置かれていればそれを返す */
    @Nullable
    private static ItemStack findPattern(final CraftingInput input) {
        ItemStack found = null;
        for (int i = 0; i < input.size(); i++) {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (found != null) {
                return null;
            }
            if (PatternUtil.isManaitaCraftingPattern(stack) && stack.has(AEComponents.ENCODED_CRAFTING_PATTERN)) {
                found = stack;
            } else if (PatternUtil.isManaitaProcessingPattern(stack) && stack.has(AEComponents.ENCODED_PROCESSING_PATTERN)) {
                found = stack;
            } else {
                return null;
            }
        }
        return found;
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        return findPattern(input) != null;
    }

    @Override
    public ItemStack assemble(final CraftingInput input, final HolderLookup.Provider registries) {
        final ItemStack pattern = findPattern(input);
        if (pattern == null) {
            return ItemStack.EMPTY;
        }
        if (PatternUtil.isManaitaCraftingPattern(pattern)) {
            final ItemStack result = AEItems.CRAFTING_PATTERN.stack();
            result.set(AEComponents.ENCODED_CRAFTING_PATTERN, pattern.get(AEComponents.ENCODED_CRAFTING_PATTERN));
            return result;
        }
        final ItemStack result = AEItems.PROCESSING_PATTERN.stack();
        result.set(AEComponents.ENCODED_PROCESSING_PATTERN, pattern.get(AEComponents.ENCODED_PROCESSING_PATTERN));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.MANAITA_PATTERN_DECODE.get();
    }
}

package dev.manaita.appliedmanaita.recipe;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import appeng.core.definitions.AEItems;

import dev.manaita.appliedmanaita.ModRegistry;
import dev.manaita.appliedmanaita.crafting.ManaitaPatternConverter;
import dev.manaita.appliedmanaita.crafting.PatternUtil;

/**
 * まな板パターンを作業台に1個だけ置く → 元の AE2 のパターンに戻す。
 * 中身(NBT)はそのまま写すので、まな板クラフトパターンは AE2 のクラフトパターン、まな板加工パターンは AE2 の加工パターンになる。
 */
public class ManaitaPatternDecodeRecipe extends CustomRecipe {

    public ManaitaPatternDecodeRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    /** 中身の入ったまな板パターンがちょうど1個だけ置かれていればそれを返す */
    @Nullable
    private static ItemStack findPattern(final CraftingContainer container) {
        ItemStack found = null;
        for (int i = 0; i < container.getContainerSize(); i++) {
            final ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (found != null) {
                return null;
            }
            if ((PatternUtil.isManaitaCraftingPattern(stack) || PatternUtil.isManaitaProcessingPattern(stack)) && stack.hasTag()) {
                found = stack;
            } else {
                return null;
            }
        }
        return found;
    }

    @Override
    public boolean matches(final CraftingContainer container, final Level level) {
        return findPattern(container) != null;
    }

    @Override
    public ItemStack assemble(final CraftingContainer container, final RegistryAccess registries) {
        final ItemStack pattern = findPattern(container);
        if (pattern == null) {
            return ItemStack.EMPTY;
        }
        final var target = PatternUtil.isManaitaCraftingPattern(pattern) ? AEItems.CRAFTING_PATTERN : AEItems.PROCESSING_PATTERN;
        return ManaitaPatternConverter.withTag(target.asItem(), 1, pattern.getTag());
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

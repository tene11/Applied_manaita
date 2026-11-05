package dev.manaita.appliedmanaita.recipe;

import appeng.api.AEApi;
import dev.manaita.appliedmanaita.ModRegistry;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * 「まな板パターン」(クラフト用・かまど用・醸造用のどれでも)を1個だけ置く → 元の AE2 の「エンコード済みパターン」に戻す。
 * 中身(NBT)はそのまま写すので、クラフト用は AE2 のクラフトパターン、かまど用・醸造用は AE2 の加工パターンになる。
 */
public class RecipeManaitaPatternDecode extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private ItemStack findPattern(final InventoryCrafting inv) {
        ItemStack found = null;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            final ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (found != null || stack.getItem() != ModRegistry.MANAITA_PATTERN || !stack.hasTagCompound()) {
                return null;
            }
            found = stack;
        }
        return found;
    }

    @Override
    public boolean matches(final InventoryCrafting inv, final World world) {
        return this.findPattern(inv) != null;
    }

    @Override
    public ItemStack getCraftingResult(final InventoryCrafting inv) {
        final ItemStack pattern = this.findPattern(inv);
        if (pattern == null) {
            return ItemStack.EMPTY;
        }
        return AEApi.instance().definitions().items().encodedPattern().maybeStack(1)
                .map(result -> {
                    result.setTagCompound(pattern.getTagCompound().copy());
                    return result;
                })
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canFit(final int width, final int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}

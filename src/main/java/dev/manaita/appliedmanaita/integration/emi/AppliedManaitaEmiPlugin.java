package dev.manaita.appliedmanaita.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;

import net.minecraft.world.item.ItemStack;

import dev.manaita.appliedmanaita.ModRegistry;

/**
 * EMI 連携。EMI が入っているときだけ、EMI がこのクラスを読み込む。
 * レシピの「作業場所」に、まな板パターンターミナルと まな板の機械を出す。
 * (まな板パターンターミナルへのレシピ転送は、AE2 のパターンエンコーディングターミナルの仕組みがそのまま効く)
 */
@EmiEntrypoint
public class AppliedManaitaEmiPlugin implements EmiPlugin {

    @Override
    public void register(final EmiRegistry registry) {
        final EmiStack terminal = EmiStack.of(new ItemStack(ModRegistry.MANAITA_PATTERN_TERMINAL.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, terminal);
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, terminal);
        registry.addWorkstation(VanillaEmiRecipeCategories.BREWING, terminal);
        registry.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, EmiStack.of(new ItemStack(ModRegistry.MANAITA_ASSEMBLER_ITEM.get())));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(new ItemStack(ModRegistry.MANAITA_MOLECULAR_SMELTER_ITEM.get())));
        registry.addWorkstation(VanillaEmiRecipeCategories.BREWING, EmiStack.of(new ItemStack(ModRegistry.MANAITA_MOLECULAR_BREWER_ITEM.get())));
    }
}

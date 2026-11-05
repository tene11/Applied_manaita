package dev.manaita.appliedmanaita.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.ModRegistry;

/**
 * JEI 連携。JEI が入っているときだけ、JEI がこのクラスを読み込む。
 * レシピの「作れる場所」に、まな板パターンターミナルと まな板の機械を出す。
 * (AE2 1.21.1 は JEI からパターンへのレシピ転送には対応していないため、転送は EMI を使う)
 */
@JeiPlugin
public class AppliedManaitaJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = AppliedManaita.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeCatalysts(final IRecipeCatalystRegistration registration) {
        final ItemStack terminal = new ItemStack(ModRegistry.MANAITA_PATTERN_TERMINAL.get());
        // 作業台レシピ: まな板パターンターミナル(クラフトモード) と まな板分子組立機
        registration.addRecipeCatalyst(terminal, RecipeTypes.CRAFTING, RecipeTypes.SMELTING, RecipeTypes.BREWING);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.MANAITA_ASSEMBLER_ITEM.get()), RecipeTypes.CRAFTING);
        // かまどレシピ: まな板分子精錬機 / 醸造レシピ: まな板分子醸造機
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.MANAITA_MOLECULAR_SMELTER_ITEM.get()), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(ModRegistry.MANAITA_MOLECULAR_BREWER_ITEM.get()), RecipeTypes.BREWING);
    }
}

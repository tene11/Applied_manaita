package dev.manaita.appliedmanaita.integration.jei;

import dev.manaita.appliedmanaita.ModRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;

/**
 * JEI(HEI)連携。JEIが入っているときだけ、JEIがこのクラスを読み込む。
 * (まな板パターンターミナルへのレシピ転送やドラッグ&ドロップは、AE2 のパターンターミナルの仕組みがそのまま効く)
 */
@JEIPlugin
public class AppliedManaitaJeiPlugin implements IModPlugin {

    @Override
    public void register(final IModRegistry registry) {
        // JEIで作業台レシピを見たとき、「作れる場所」にまな板パターンターミナルも出す
        registry.addRecipeCatalyst(new ItemStack(ModRegistry.MANAITA_PATTERN_TERMINAL), VanillaRecipeCategoryUid.CRAFTING);
        // かまどレシピには、まな板分子精錬機とまな板パターンターミナル(加工モードで かまど用のまな板パターン を作れる)を出す
        registry.addRecipeCatalyst(new ItemStack(ModRegistry.MANAITA_MOLECULAR_SMELTER), VanillaRecipeCategoryUid.SMELTING);
        registry.addRecipeCatalyst(new ItemStack(ModRegistry.MANAITA_PATTERN_TERMINAL), VanillaRecipeCategoryUid.SMELTING);
        // 醸造レシピには、まな板分子醸造機とまな板パターンターミナル(加工モードで 醸造用のまな板パターン を作れる)を出す
        registry.addRecipeCatalyst(new ItemStack(ModRegistry.MANAITA_MOLECULAR_BREWER), VanillaRecipeCategoryUid.BREWING);
        registry.addRecipeCatalyst(new ItemStack(ModRegistry.MANAITA_PATTERN_TERMINAL), VanillaRecipeCategoryUid.BREWING);
    }
}

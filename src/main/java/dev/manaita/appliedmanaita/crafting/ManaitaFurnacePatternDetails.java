package dev.manaita.appliedmanaita.crafting;

import appeng.helpers.PatternHelper;
import net.minecraft.item.ItemStack;

/**
 * かまど用のまな板パターンの中身。
 * 「素材1種類 → そのかまどレシピの完成品」(個数も比率どおり、{@link PatternUtil#isSmeltingPattern})になっているものだけが有効で、
 * 出力は まな板+ の調理まな板の倍率(furnaceDoubling)倍。まな板分子精錬機 が受け取る。
 * 作るときは {@link ManaitaProcessingPatternDetails#create} を使う。
 */
public class ManaitaFurnacePatternDetails extends ManaitaProcessingPatternDetails {

    ManaitaFurnacePatternDetails(final ItemStack is, final PatternHelper base) {
        super(is, base);
    }

    @Override
    protected int readMultiplier() {
        return ManaitaMultiplier.getFurnaceMultiplier();
    }

    @Override
    public String getMultiplierTooltipKey() {
        return "tooltip.appliedmanaita.furnace_multiplier";
    }
}

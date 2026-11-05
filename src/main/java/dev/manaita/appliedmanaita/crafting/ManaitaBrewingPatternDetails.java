package dev.manaita.appliedmanaita.crafting;

import appeng.helpers.PatternHelper;
import net.minecraft.item.ItemStack;

/**
 * 醸造用のまな板パターンの中身。
 * 「材料1個 ＋ 元のポーション1〜3本 → 醸造の完成品(本数は元のポーションと同じ)」になっているものだけが有効
 * ({@link PatternUtil#isBrewingPattern})。出力は まな板+ の醸造まな板の倍率(brewingDoubling)倍。
 * まな板分子醸造機 が受け取る。作るときは {@link ManaitaProcessingPatternDetails#create} を使う。
 * <p>
 * 強化・延長などの段階的なポーションは、段階ごとにパターンを作れば AE2 が自動でつないで作る。
 */
public class ManaitaBrewingPatternDetails extends ManaitaProcessingPatternDetails {

    ManaitaBrewingPatternDetails(final ItemStack is, final PatternHelper base) {
        super(is, base);
    }

    @Override
    protected int readMultiplier() {
        return ManaitaMultiplier.getBrewingMultiplier();
    }

    @Override
    public String getMultiplierTooltipKey() {
        return "tooltip.appliedmanaita.brewing_multiplier";
    }
}

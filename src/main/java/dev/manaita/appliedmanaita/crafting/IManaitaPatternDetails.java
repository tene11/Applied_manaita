package dev.manaita.appliedmanaita.crafting;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import net.minecraft.item.ItemStack;

/**
 * まな板パターンの中身(クラフト用・かまど用・醸造用)に共通するもの。
 * 出力は「1回分の完成品 × まな板+ の倍率」になる。
 */
public interface IManaitaPatternDetails extends ICraftingPatternDetails {

    /** 1回分の完成品(倍率をかける前) */
    ItemStack getBaseOutput();

    /** 今の倍率(まな板+ のコンフィグから読む) */
    int getMultiplier();

    /** 説明欄に出す倍率の行の翻訳キー(引数は倍率) */
    String getMultiplierTooltipKey();
}

package dev.manaita.appliedmanaita.tile;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import dev.manaita.appliedmanaita.crafting.ManaitaBrewingPatternDetails;

/**
 * まな板分子醸造機。MEインターフェースの隣に置いて使う。
 * <p>
 * インターフェースに入っている「まな板パターン(醸造用)」の素材(材料1個 ＋ 元のポーション1〜3本)を受け取ると、
 * 燃料(ブレイズパウダー)なしで一瞬で醸造し、まな板+ の醸造まな板の倍率をかけた数の完成品を ME ネットワークに直接入れる。
 * 動きは {@link TileManaitaInstantProcessor} にまとめてある。
 */
public class TileManaitaMolecularBrewer extends TileManaitaInstantProcessor {

    @Override
    protected boolean acceptsPattern(final ICraftingPatternDetails details) {
        return details instanceof ManaitaBrewingPatternDetails;
    }

    @Override
    protected String logName() {
        return "まな板分子醸造機";
    }
}

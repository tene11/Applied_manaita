package dev.manaita.appliedmanaita.tile;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import dev.manaita.appliedmanaita.crafting.ManaitaFurnacePatternDetails;

/**
 * まな板分子精錬機。MEインターフェースの隣に置いて使う。
 * <p>
 * インターフェースに入っている「まな板パターン(かまど用)」の素材を受け取ると、燃料なしで一瞬で焼き、
 * まな板+ の調理まな板の倍率をかけた数の完成品を ME ネットワークに直接入れる。
 * 動きは {@link TileManaitaInstantProcessor} にまとめてある。
 */
public class TileManaitaMolecularSmelter extends TileManaitaInstantProcessor {

    @Override
    protected boolean acceptsPattern(final ICraftingPatternDetails details) {
        return details instanceof ManaitaFurnacePatternDetails;
    }

    @Override
    protected String logName() {
        return "まな板分子精錬機";
    }
}

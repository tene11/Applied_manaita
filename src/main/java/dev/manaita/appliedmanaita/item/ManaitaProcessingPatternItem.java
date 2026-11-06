package dev.manaita.appliedmanaita.item;

import net.minecraft.world.level.Level;

import appeng.api.stacks.AEItemKey;

import dev.manaita.appliedmanaita.crafting.IManaitaPattern;
import dev.manaita.appliedmanaita.crafting.ManaitaProcessingPattern;

/**
 * まな板加工パターンのアイテム(中身は AE2 の加工パターンと同じ)。かまどレシピか醸造レシピだけが有効で、
 * 出力は調理まな板／醸造まな板の倍率倍。
 */
public class ManaitaProcessingPatternItem extends ManaitaPatternItem {

    public ManaitaProcessingPatternItem(final Properties properties) {
        super(properties);
    }

    @Override
    protected IManaitaPattern create(final AEItemKey what, final Level level) {
        return new ManaitaProcessingPattern(what, level);
    }
}

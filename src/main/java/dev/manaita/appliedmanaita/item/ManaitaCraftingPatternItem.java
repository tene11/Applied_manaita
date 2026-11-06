package dev.manaita.appliedmanaita.item;

import net.minecraft.world.level.Level;

import appeng.api.stacks.AEItemKey;

import dev.manaita.appliedmanaita.crafting.IManaitaPattern;
import dev.manaita.appliedmanaita.crafting.ManaitaCraftingPattern;

/**
 * まな板クラフトパターンのアイテム(中身は AE2 のクラフトパターンと同じ)。出力は作業まな板の倍率倍。
 */
public class ManaitaCraftingPatternItem extends ManaitaPatternItem {

    public ManaitaCraftingPatternItem(final Properties properties) {
        super(properties);
    }

    @Override
    protected IManaitaPattern create(final AEItemKey what, final Level level) {
        return new ManaitaCraftingPattern(what, level);
    }
}

package dev.manaita.appliedmanaita.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedCraftingPattern;
import appeng.crafting.pattern.EncodedProcessingPattern;

import dev.manaita.appliedmanaita.ModRegistry;

/**
 * まな板パターンターミナルでエンコードしたとき、AE2 のパターンを まな板パターン に差し替える。
 * <ul>
 *     <li>クラフトパターン → まな板クラフトパターン(中身はそのまま)</li>
 *     <li>加工パターンでかまどレシピ(素材1種類 → その完成品) → まな板加工パターン(中身はそのまま)</li>
 *     <li>加工パターンで醸造レシピ → まな板加工パターン(材料1個 ＋ 元のポーションn本 → 完成品n本 の形に直す)</li>
 *     <li>それ以外(普通の加工・鍛冶台・石切台) → 普通の AE2 のパターンのまま</li>
 * </ul>
 */
public final class ManaitaPatternConverter {

    private ManaitaPatternConverter() {
    }

    public static ItemStack toManaita(final ItemStack encoded, final Level level) {
        if (encoded.isEmpty()) {
            return encoded;
        }
        if (AEItems.CRAFTING_PATTERN.is(encoded)) {
            final EncodedCraftingPattern crafting = encoded.get(AEComponents.ENCODED_CRAFTING_PATTERN);
            if (crafting == null) {
                return encoded;
            }
            final ItemStack result = new ItemStack(ModRegistry.MANAITA_CRAFTING_PATTERN.get(), encoded.getCount());
            result.set(AEComponents.ENCODED_CRAFTING_PATTERN, crafting);
            return result;
        }
        if (AEItems.PROCESSING_PATTERN.is(encoded)) {
            final EncodedProcessingPattern processing = encoded.get(AEComponents.ENCODED_PROCESSING_PATTERN);
            if (processing == null) {
                return encoded;
            }
            final AEProcessingPattern base;
            try {
                base = new AEProcessingPattern(AEItemKey.of(encoded));
            } catch (final RuntimeException e) {
                return encoded; // 中身が読めない
            }
            EncodedProcessingPattern converted = null;
            if (PatternUtil.isSmeltingPattern(base, level)) {
                converted = processing;
            } else {
                converted = PatternUtil.toBrewingPattern(base, level);
            }
            if (converted == null) {
                return encoded; // かまどレシピでも醸造レシピでもない → 普通の加工パターン
            }
            final ItemStack result = new ItemStack(ModRegistry.MANAITA_PROCESSING_PATTERN.get(), encoded.getCount());
            result.set(AEComponents.ENCODED_PROCESSING_PATTERN, converted);
            return result;
        }
        return encoded;
    }
}

package dev.manaita.appliedmanaita.crafting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;

import dev.manaita.appliedmanaita.ModRegistry;

/**
 * まな板パターンターミナルでエンコードしたとき、AE2 のパターンを まな板パターン に差し替える。
 * <ul>
 *     <li>クラフトパターン → まな板クラフトパターン(中身はそのまま)</li>
 *     <li>加工パターンでかまどレシピ(素材1種類 → その完成品) → まな板加工パターン(中身はそのまま)</li>
 *     <li>加工パターンで醸造レシピ → まな板加工パターン(材料1個 ＋ 元のポーションn本 → 完成品n本 の形に直す)</li>
 *     <li>それ以外(普通の加工・鍛冶台・石切台) → 普通の AE2 のパターンのまま</li>
 * </ul>
 * パターンの中身は AE2 と同じく、アイテムの NBT に入っている。
 */
public final class ManaitaPatternConverter {

    private ManaitaPatternConverter() {
    }

    public static ItemStack toManaita(final ItemStack encoded, final Level level) {
        if (encoded.isEmpty() || encoded.getTag() == null) {
            return encoded;
        }
        if (AEItems.CRAFTING_PATTERN.isSameAs(encoded)) {
            return withTag(ModRegistry.MANAITA_CRAFTING_PATTERN.get(), encoded.getCount(), encoded.getTag());
        }
        if (AEItems.PROCESSING_PATTERN.isSameAs(encoded)) {
            final AEProcessingPattern base;
            try {
                base = new AEProcessingPattern(AEItemKey.of(encoded));
            } catch (final RuntimeException e) {
                return encoded; // 中身が読めない
            }
            if (PatternUtil.isSmeltingPattern(base, level)) {
                return withTag(ModRegistry.MANAITA_PROCESSING_PATTERN.get(), encoded.getCount(), encoded.getTag());
            }
            final ItemStack brewing = PatternUtil.toBrewingPattern(base);
            if (brewing == null || brewing.getTag() == null) {
                return encoded; // かまどレシピでも醸造レシピでもない → 普通の加工パターン
            }
            return withTag(ModRegistry.MANAITA_PROCESSING_PATTERN.get(), encoded.getCount(), brewing.getTag());
        }
        return encoded;
    }

    /** アイテムを差し替えて、パターンの中身(NBT)を写す */
    public static ItemStack withTag(final Item item, final int count, @Nullable final CompoundTag tag) {
        final ItemStack result = new ItemStack(item, count);
        if (tag != null) {
            result.setTag(tag.copy());
        }
        return result;
    }
}

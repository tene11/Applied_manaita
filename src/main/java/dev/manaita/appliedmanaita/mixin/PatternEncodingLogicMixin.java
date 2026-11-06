package dev.manaita.appliedmanaita.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.parts.encoding.PatternEncodingLogic;

import dev.manaita.appliedmanaita.crafting.IManaitaPattern;

/**
 * パターンエンコーディングターミナル(まな板パターンターミナルも含む)の出口に まな板パターン を入れたとき、
 * そのレシピを枠に読み込めるようにする。
 * AE2 は自分のクラフトパターン・加工パターンの中身しか読み込まないので、まな板パターンは中の AE2 のパターン
 * (倍率をかける前のもの)を渡す。
 * <p>
 * 1.20.1 の Forge には MixinExtras が入っていないので、Mixin 標準の @Redirect を使う。
 * 書き換える先は AE2 のメソッドだけなので、名前の変換(remap)はしない。
 */
@Mixin(value = PatternEncodingLogic.class, remap = false)
public abstract class PatternEncodingLogicMixin {

    @Redirect(method = "loadEncodedPattern", at = @At(value = "INVOKE",
            target = "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"))
    private IPatternDetails appliedmanaita$unwrapManaitaPattern(final ItemStack stack, final Level level) {
        final IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
        return details instanceof IManaitaPattern manaita ? manaita.getBase() : details;
    }
}

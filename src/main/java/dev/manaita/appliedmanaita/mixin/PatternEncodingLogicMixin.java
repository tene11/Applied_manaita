package dev.manaita.appliedmanaita.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.parts.encoding.PatternEncodingLogic;

import dev.manaita.appliedmanaita.crafting.IManaitaPattern;

/**
 * パターンエンコーディングターミナル(まな板パターンターミナルも含む)の出口に まな板パターン を入れたとき、
 * そのレシピを枠に読み込めるようにする。
 * AE2 は自分のクラフトパターン・加工パターンの中身しか読み込まないので、まな板パターンは中の AE2 のパターン
 * (倍率をかける前のもの)を渡す。
 */
@Mixin(value = PatternEncodingLogic.class, remap = false)
public abstract class PatternEncodingLogicMixin {

    @WrapOperation(method = "loadEncodedPattern", at = @At(value = "INVOKE",
            target = "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"))
    private IPatternDetails appliedmanaita$unwrapManaitaPattern(final ItemStack stack, final Level level,
                                                               final Operation<IPatternDetails> original) {
        final IPatternDetails details = original.call(stack, level);
        return details instanceof IManaitaPattern manaita ? manaita.getBase() : details;
    }
}

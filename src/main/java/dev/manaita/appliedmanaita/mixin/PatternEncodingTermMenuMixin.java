package dev.manaita.appliedmanaita.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.item.ItemStack;

import appeng.menu.me.items.PatternEncodingTermMenu;

import dev.manaita.appliedmanaita.crafting.ManaitaPatternConverter;
import dev.manaita.appliedmanaita.part.ManaitaPatternTerminalPart;

/**
 * AE2 のパターンエンコーディングターミナルの「エンコード」を、まな板パターンターミナルのときだけ書き換える。
 * <ul>
 *     <li>クラフトモード: できたパターンを まな板クラフトパターン にする(中身はそのまま)</li>
 *     <li>加工モード: かまどレシピ・醸造レシピなら まな板加工パターン、それ以外は普通の AE2 の加工パターン</li>
 * </ul>
 * 出口に まな板パターン があっても、AE2 のパターンと同じく上書きでエンコードできる(AE2 がエンコード済みパターンとして扱うため)。
 * 普通のパターンエンコーディングターミナル(ワイヤレス含む)には何もしない。
 * <p>
 * ほかの mod の書き換えと重ねがけできるよう、MixinExtras の @WrapOperation を使う。
 */
@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin {

    @WrapOperation(method = "encode", at = @At(value = "INVOKE",
            target = "Lappeng/menu/me/items/PatternEncodingTermMenu;encodePattern()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack appliedmanaita$toManaitaPattern(final PatternEncodingTermMenu menu, final Operation<ItemStack> original) {
        final ItemStack encoded = original.call(menu);
        if (encoded == null || !(menu.getHost() instanceof ManaitaPatternTerminalPart)) {
            return encoded;
        }
        return ManaitaPatternConverter.toManaita(encoded, menu.getPlayer().level());
    }
}

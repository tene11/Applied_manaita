package dev.manaita.appliedmanaita.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
 * 1.20.1 の Forge には MixinExtras が入っていないので、Mixin 標準の @Redirect を使う。
 * 書き換える先は AE2 のメソッドだけなので、名前の変換(remap)はしない。
 */
@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin {

    /** AE2 の private なメソッド(今のモードでパターンを作る) */
    @Shadow
    @Nullable
    private ItemStack encodePattern() {
        throw new AssertionError();
    }

    @Redirect(method = "encode", at = @At(value = "INVOKE",
            target = "Lappeng/menu/me/items/PatternEncodingTermMenu;encodePattern()Lnet/minecraft/world/item/ItemStack;"))
    @Nullable
    private ItemStack appliedmanaita$toManaitaPattern(final PatternEncodingTermMenu menu) {
        final ItemStack encoded = this.encodePattern();
        if (encoded == null || !(menu.getHost() instanceof ManaitaPatternTerminalPart)) {
            return encoded;
        }
        return ManaitaPatternConverter.toManaita(encoded, menu.getPlayer().level());
    }
}

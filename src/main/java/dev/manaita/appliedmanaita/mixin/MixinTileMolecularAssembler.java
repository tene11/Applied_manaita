package dev.manaita.appliedmanaita.mixin;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.items.misc.ItemEncodedPattern;
import appeng.tile.crafting.TileMolecularAssembler;
import dev.manaita.appliedmanaita.crafting.ManaitaPatternDetails;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 普通の AE2 分子組立機では、まな板パターンを使わせない。
 * (まな板パターンは AE2 と同じく「クラフトパターン」として扱うので、何もしないと普通の分子組立機も受け取ってしまう)
 */
@Mixin(value = TileMolecularAssembler.class, remap = false)
public abstract class MixinTileMolecularAssembler {

    /** MEインターフェースから渡されても断る(断るとインターフェースは隣の まな板分子組立機 を探す) */
    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true)
    private void appliedmanaita$rejectManaitaPattern(final ICraftingPatternDetails details, final InventoryCrafting table,
                                                     final EnumFacing where, final CallbackInfoReturnable<Boolean> cir) {
        if (details instanceof ManaitaPatternDetails) {
            cir.setReturnValue(false);
        }
    }

    /** 分子組立機のパターン欄に入れられても、まな板パターンは使えないパターンとして扱う */
    @Redirect(method = "recalculatePlan", at = @At(value = "INVOKE",
            target = "Lappeng/items/misc/ItemEncodedPattern;getPatternForItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;)Lappeng/api/networking/crafting/ICraftingPatternDetails;"))
    private ICraftingPatternDetails appliedmanaita$ignoreManaitaPattern(final ItemEncodedPattern item, final ItemStack is, final World w) {
        final ICraftingPatternDetails details = item.getPatternForItem(is, w);
        return details instanceof ManaitaPatternDetails ? null : details;
    }
}

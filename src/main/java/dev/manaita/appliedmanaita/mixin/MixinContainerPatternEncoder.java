package dev.manaita.appliedmanaita.mixin;

import appeng.api.AEApi;
import appeng.container.implementations.ContainerPatternEncoder;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.PatternHelper;
import appeng.parts.reporting.AbstractPartEncoder;
import dev.manaita.appliedmanaita.ModRegistry;
import dev.manaita.appliedmanaita.crafting.PatternUtil;
import dev.manaita.appliedmanaita.part.PartManaitaPatternTerminal;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 のパターンターミナルの「エンコード」を、まな板パターンターミナルのときだけ書き換える。
 * <ul>
 *     <li>クラフトモード: できたパターンを まな板パターン にする(中の印は crafting=true のまま)</li>
 *     <li>加工モード: かまどレシピ(素材1種類 → その完成品)なら まな板パターン(かまど用)、
 *     醸造レシピ(材料1個 ＋ 元のポーション1〜3本 → 完成品)なら まな板パターン(醸造用)、
 *     それ以外は普通の AE2 の加工パターン。どちらも crafting=false</li>
 *     <li>醸造レシピは JEI から転送すると「完成品1本」「燃料のブレイズパウダー入り」になるので、
 *     完成品の本数を元のポーションの本数に合わせ、燃料を外した形に直す({@link PatternUtil#toBrewingPatternTag})</li>
 *     <li>出口に まな板パターン があっても、AE2 のパターンと同じく上書きでエンコードできるようにする</li>
 * </ul>
 * AE2 のエンコード済みパターンと同じく、まな板パターンも1種類のアイテムで、使い道は中の印で決まる。
 * 普通のパターンターミナル(ワイヤレス含む)には何もしない。
 */
@Mixin(value = ContainerPatternEncoder.class, remap = false)
public abstract class MixinContainerPatternEncoder {

    @Shadow
    protected SlotRestrictedInput patternSlotOUT;

    @Shadow
    public abstract AbstractPartEncoder getPart();

    /** 出口の まな板パターン も「上書きしてよいパターン」として扱う */
    @Inject(method = "isPattern", at = @At("HEAD"), cancellable = true)
    private void appliedmanaita$acceptManaitaPattern(final ItemStack output, final CallbackInfoReturnable<Boolean> cir) {
        if (this.getPart() instanceof PartManaitaPatternTerminal && PatternUtil.isManaitaPatternItem(output)) {
            cir.setReturnValue(true);
        }
    }

    /** エンコードし終わったら、出口のパターンをモードに合ったアイテムに差し替える(中身のNBTは、醸造レシピのときだけ形を直す) */
    @Inject(method = "encode", at = @At("TAIL"))
    private void appliedmanaita$convertEncodedPattern(final CallbackInfo ci) {
        if (!(this.getPart() instanceof PartManaitaPatternTerminal)) {
            return;
        }
        final ItemStack out = this.patternSlotOUT.getStack();
        if (out.isEmpty() || !out.hasTagCompound()) {
            return;
        }

        NBTTagCompound tag = out.getTagCompound().copy();
        final boolean crafting = tag.getBoolean("crafting");
        final ItemStack wanted;
        if (crafting || this.appliedmanaita$isSmelting(out)) {
            // クラフト用もかまど用も同じ まな板パターン。違いは中の印(crafting)と中身だけ
            wanted = new ItemStack(ModRegistry.MANAITA_PATTERN, out.getCount());
        } else {
            final NBTTagCompound brewing = this.appliedmanaita$toBrewing(out);
            if (brewing != null) {
                wanted = new ItemStack(ModRegistry.MANAITA_PATTERN, out.getCount());
                tag = brewing;
            } else {
                wanted = AEApi.instance().definitions().items().encodedPattern().maybeStack(out.getCount()).orElse(ItemStack.EMPTY);
            }
        }
        if (wanted.isEmpty() || (wanted.getItem() == out.getItem() && tag.equals(out.getTagCompound()))) {
            return;
        }
        wanted.setTagCompound(tag);
        this.patternSlotOUT.putStack(wanted);
    }

    /** 加工パターンが醸造レシピなら、醸造用のまな板パターンの中身(NBT)を返す。違えば null */
    private NBTTagCompound appliedmanaita$toBrewing(final ItemStack pattern) {
        final TileEntity te = this.getPart().getTile();
        if (te == null || te.getWorld() == null) {
            return null;
        }
        try {
            return PatternUtil.toBrewingPatternTag(new PatternHelper(pattern, te.getWorld()));
        } catch (final RuntimeException e) {
            return null; // 中身が空などで読めない
        }
    }

    /** 加工パターンが「素材1種類 → そのかまどレシピの完成品」になっているか */
    private boolean appliedmanaita$isSmelting(final ItemStack pattern) {
        final TileEntity te = this.getPart().getTile();
        if (te == null || te.getWorld() == null) {
            return false;
        }
        try {
            return PatternUtil.isSmeltingPattern(new PatternHelper(pattern, te.getWorld()));
        } catch (final RuntimeException e) {
            return false; // 中身が空などで読めない
        }
    }
}

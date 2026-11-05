package dev.manaita.appliedmanaita.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;

import dev.manaita.appliedmanaita.MixinCheck;
import dev.manaita.appliedmanaita.blockentity.ManaitaAssemblerBlockEntity;
import dev.manaita.appliedmanaita.blockentity.ManaitaInstantProcessorBlockEntity;
import dev.manaita.appliedmanaita.crafting.IManaitaPattern;

/**
 * パターンプロバイダーが まな板パターン の素材を、まな板の機械以外へ送らないようにする。
 * <p>
 * チェストや別のネットワークへ送らないのは、まな板パターン自身が
 * {@link IPatternDetails#supportsPushInputsToExternalInventory()} を false にしているので AE2 が守ってくれる。
 * ここでは、ほかの mod の機械(ICraftingMachine)が まな板パターンを受け取ってしまわないようにする
 * (受け取られると、CPU は倍率倍の完成品を待ち続け、注文が終わらなくなる)。
 * 普通の AE2 のパターンの動きは変えない。
 * <p>
 * {@link MixinCheck.Marker} を付けるのは、起動時に「Mixin が効いているか」を確かめるため。
 */
@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin implements MixinCheck.Marker {

    /** いま渡そうとしているのが まな板パターン か(pushPattern の間だけ true) */
    @Unique
    private boolean appliedmanaita$pushingManaita;

    @Inject(method = "pushPattern", at = @At("HEAD"))
    private void appliedmanaita$beginPush(final IPatternDetails details, final KeyCounter[] inputHolder,
                                         final CallbackInfoReturnable<Boolean> cir) {
        this.appliedmanaita$pushingManaita = details instanceof IManaitaPattern;
    }

    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void appliedmanaita$endPush(final IPatternDetails details, final KeyCounter[] inputHolder,
                                       final CallbackInfoReturnable<Boolean> cir) {
        this.appliedmanaita$pushingManaita = false;
    }

    /** まな板の機械以外の機械へ渡さない */
    @WrapOperation(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/api/implementations/blockentities/ICraftingMachine;acceptsPlans()Z"))
    private boolean appliedmanaita$onlyManaitaMachines(final ICraftingMachine machine, final Operation<Boolean> original) {
        if (this.appliedmanaita$pushingManaita
                && !(machine instanceof ManaitaAssemblerBlockEntity)
                && !(machine instanceof ManaitaInstantProcessorBlockEntity)) {
            return false;
        }
        return original.call(machine);
    }
}

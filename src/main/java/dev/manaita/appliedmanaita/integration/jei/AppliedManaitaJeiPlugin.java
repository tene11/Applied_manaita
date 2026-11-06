package dev.manaita.appliedmanaita.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import net.minecraft.resources.ResourceLocation;

import dev.manaita.appliedmanaita.AppliedManaita;

/**
 * JEI 連携。JEI が入っているときだけ、JEI がこのクラスを読み込む。
 * <ul>
 *     <li>レシピの「作れる場所」には何も出さない(AE2 本家のターミナルも出していないのに合わせる)</li>
 *     <li>JEI の「+」ボタンで、レシピをパターンエンコーディングターミナル(まな板パターンターミナル含む)に転送できるようにする。
 *     AE2 1.21.1 は EMI 連携しか持っていないため、ここで自前の転送処理(EncodePatternTransferHandler)を登録する</li>
 * </ul>
 */
@JeiPlugin
public class AppliedManaitaJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = AppliedManaita.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(final IRecipeTransferRegistration registration) {
        // どのレシピの種類でも使える転送処理として登録する(作業台・かまど・醸造台・ほかの mod の機械)
        registration.addUniversalRecipeTransferHandler(new EncodePatternTransferHandler(registration.getTransferHelper()));
    }
}

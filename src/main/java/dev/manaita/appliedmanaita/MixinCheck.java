package dev.manaita.appliedmanaita;

import appeng.helpers.patternprovider.PatternProviderLogic;

/**
 * Mixin がきちんと効いているかの確認。
 * <p>
 * Mixin が効かないと、まな板パターンの素材がほかの mod の機械へ流れ、
 * CPU が倍率倍の完成品を待ち続けて注文が終わらなくなる。
 * 起動時に確かめて、効いていなければ分かりやすいメッセージで止める。
 */
public final class MixinCheck {

    /** Mixin で AE2 の PatternProviderLogic に付ける印(mixin/PatternProviderLogicMixin) */
    public interface Marker {
    }

    private MixinCheck() {
    }

    /** 起動時に呼ぶ。Mixin が効いていなければ起動を止める */
    static void verify() {
        if (!Marker.class.isAssignableFrom(PatternProviderLogic.class)) {
            throw new IllegalStateException("[Applied Manaita] Mixin が適用されていません。AE2 のバージョンが対応しているか確かめてください。"
                    + " / Mixins were not applied. Please check that your AE2 version is supported.");
        }
    }
}

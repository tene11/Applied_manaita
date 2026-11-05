package dev.manaita.appliedmanaita;

import appeng.helpers.DualityInterface;

/**
 * Mixin がきちんと効いているかの確認。
 * <p>
 * Mixin が効かないと、まな板パターンの素材がチェストや普通の分子組立機へ流れ、
 * CPU が倍率倍の完成品を待ち続けて注文が終わらなくなる。
 * MixinBooter が入っていないと、エラーも出ずに Mixin が全部効かないまま起動してしまうので、
 * 起動時に確かめて、効いていなければ分かりやすいメッセージで止める。
 * (MixinBooter・Cleanroom のどちらで動いていても使える確かめ方にしてある)
 */
public final class MixinCheck {

    /** Mixin で AE2 の DualityInterface に付ける印(mixin/MixinDualityInterface) */
    public interface Marker {
    }

    private MixinCheck() {
    }

    /** init で呼ぶ。Mixin が効いていなければ起動を止める */
    static void verify() {
        if (!Marker.class.isAssignableFrom(DualityInterface.class)) {
            throw new IllegalStateException("[Applied Manaita] Mixin が適用されていません。MixinBooter を入れてください(Cleanroom なら不要です)。"
                    + " / Mixins were not applied. Please install MixinBooter (not needed on Cleanroom).");
        }
    }
}

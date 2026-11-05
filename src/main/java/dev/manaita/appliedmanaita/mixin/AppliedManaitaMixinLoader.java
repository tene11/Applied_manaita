package dev.manaita.appliedmanaita.mixin;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

/**
 * AE2 のクラスを書き換える Mixin の設定を MixinBooter に渡す。
 * (MixinBooter がこのクラスを自動で見つけて呼ぶ。開発環境でも本番でも動く)
 */
@SuppressWarnings("deprecation")
public class AppliedManaitaMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.appliedmanaita.json");
    }
}

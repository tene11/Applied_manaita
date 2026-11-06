package dev.manaita.appliedmanaita.crafting;

import java.util.ArrayList;
import java.util.List;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;

/**
 * まな板パターン(クラフト用・加工用)の中身に共通するもの。
 * <p>
 * 中身は AE2 のパターンと同じで、レシピの読み取りは AE2 に任せる({@link #getBase()})。
 * 違いは出力の数だけ: 「まな板mod の倍率」倍になっていて、CPU はこの数を待つ。
 * 倍率はパターンに書き込まず、その時の設定から読む。そのため、設定を変えると
 * パターンプロバイダーに入れたままのパターンも新しい倍率になる。
 * <p>
 * まな板パターンの素材は、まな板の機械(ICraftingMachine)にしか渡さない
 * ({@link #supportsPushInputsToExternalInventory()} が false。チェストなどへは送られない)。
 */
public interface IManaitaPattern extends IPatternDetails {

    /** どの倍率を使うか(作業まな板・調理まな板・醸造まな板) */
    ManaitaMultiplier.Kind getKind();

    /** 今の倍率 */
    int getMultiplier();

    /** AE2 が読み取った中身(倍率をかける前) */
    IPatternDetails getBase();

    @Override
    default boolean supportsPushInputsToExternalInventory() {
        // まな板の機械以外(チェストや別のネットワーク)へ素材を送ると、CPU は倍率倍の完成品を待ち続けてしまう
        return false;
    }

    /** 出力に倍率をかけたもの。個数は long なのであふれにくい */
    static List<GenericStack> multiply(final List<GenericStack> outputs, final int multiplier) {
        final List<GenericStack> result = new ArrayList<>(outputs.size());
        for (final GenericStack out : outputs) {
            final long amount = out.amount() > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : out.amount() * multiplier;
            result.add(new GenericStack(out.what(), amount));
        }
        return List.copyOf(result);
    }
}

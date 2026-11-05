package dev.manaita.appliedmanaita.crafting;

import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.PatternHelper;
import appeng.util.item.AEItemStack;
import net.minecraft.item.ItemStack;

/**
 * まな板パターンの中身(クラフト用・かまど用・醸造用)の共通部分。
 * <p>
 * 中身は AE2 のパターンと同じNBTで、レシピの読み取りは AE2 の {@link PatternHelper} に任せる。
 * 違いは出力の数だけ: 「まな板+ の倍率」倍になっていて、CPUはこの数を待つ。
 * 倍率はパターンに書き込まず、その時のまな板+ のコンフィグから読む({@link #readMultiplier()})。
 * そのため、コンフィグを変えると、MEインターフェースに入れたままのパターンも新しい倍率になる。
 */
public abstract class AbstractManaitaPatternDetails implements IManaitaPatternDetails {

    private final ItemStack patternStack;
    /** AE2 が読み取った中身(倍率をかける前) */
    protected final PatternHelper base;
    private final IAEItemStack patternKey;

    /** 今の倍率で計算した出力(倍率が変わったら作り直す) */
    private int multiplier;
    private IAEItemStack[] outputs;
    private IAEItemStack[] condensedOutputs;
    private int priority = 0;

    protected AbstractManaitaPatternDetails(final ItemStack patternStack, final PatternHelper base) {
        this.patternStack = patternStack;
        this.base = base;
        this.patternKey = AEItemStack.fromItemStack(patternStack);
        this.refresh();
    }

    /** 今の倍率(まな板+ のコンフィグ)。コンストラクタからも呼ばれるので、フィールドを使わないこと */
    protected abstract int readMultiplier();

    /** コンフィグの倍率が変わっていたら、出力を作り直す */
    private synchronized void refresh() {
        final int now = this.readMultiplier();
        if (this.outputs == null || now != this.multiplier) {
            this.multiplier = now;
            this.outputs = multiply(this.base.getOutputs(), now);
            this.condensedOutputs = multiply(this.base.getCondensedOutputs(), now);
        }
    }

    private static IAEItemStack[] multiply(final IAEItemStack[] src, final int multiplier) {
        final IAEItemStack[] result = new IAEItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            if (src[i] != null) {
                final IAEItemStack copy = src[i].copy();
                copy.setStackSize(src[i].getStackSize() * multiplier); // long で計算するのであふれにくい
                result[i] = copy;
            }
        }
        return result;
    }

    @Override
    public int getMultiplier() {
        this.refresh();
        return this.multiplier;
    }

    @Override
    public ItemStack getPattern() {
        return this.patternStack;
    }

    @Override
    public IAEItemStack[] getInputs() {
        return this.base.getInputs();
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return this.base.getCondensedInputs();
    }

    @Override
    public IAEItemStack[] getOutputs() {
        this.refresh();
        return this.outputs;
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        this.refresh();
        return this.condensedOutputs;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(final int priority) {
        this.priority = priority;
    }

    @Override
    public int hashCode() {
        return this.patternKey.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        return this.patternKey.equals(((AbstractManaitaPatternDetails) obj).patternKey);
    }
}

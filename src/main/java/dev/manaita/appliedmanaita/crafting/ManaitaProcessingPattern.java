package dev.manaita.appliedmanaita.crafting;

import net.minecraft.world.level.Level;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;

/**
 * まな板加工パターンの中身。AE2 の加工パターンと同じ中身で、レシピに合っているものだけが有効。
 * <ul>
 *     <li>かまどレシピ(素材1種類 → その完成品。個数も比率どおり): 出力は調理まな板の倍率倍。まな板分子精錬機 が受け取る</li>
 *     <li>醸造レシピ(材料1個 ＋ 元のポーション1〜3本 → 完成品。本数は元のポーションと同じ): 出力は醸造まな板の倍率倍。
 *     まな板分子醸造機 が受け取る</li>
 * </ul>
 * 読み込むたびにレシピと照らし合わせるので、データパックなどでレシピが変わって合わなくなったパターンは無効になる。
 * 比率の水増し(少ない素材で多くの完成品)も、これで防いでいる。
 */
public final class ManaitaProcessingPattern implements IManaitaPattern {

    private final AEProcessingPattern base;
    private final ManaitaMultiplier.Kind kind;
    private final boolean clientSide;

    private int multiplier = -1;
    private GenericStack[] outputs = new GenericStack[0];

    /**
     * @throws IllegalArgumentException かまどレシピにも醸造レシピにも合わない場合
     * @throws RuntimeException         パターンが壊れている場合(AE2 が投げる)
     */
    public ManaitaProcessingPattern(final AEItemKey definition, final Level level) {
        this.base = new AEProcessingPattern(definition);
        if (PatternUtil.isSmeltingPattern(this.base, level)) {
            this.kind = ManaitaMultiplier.Kind.FURNACE;
        } else if (PatternUtil.isBrewingPattern(this.base, level)) {
            this.kind = ManaitaMultiplier.Kind.BREWING;
        } else {
            throw new IllegalArgumentException("まな板加工パターンは、かまどレシピか醸造レシピだけです: " + definition);
        }
        this.clientSide = level.isClientSide();
        this.refresh();
    }

    private synchronized void refresh() {
        final int now = ManaitaMultiplier.get(this.kind, this.clientSide);
        if (now != this.multiplier) {
            this.multiplier = now;
            this.outputs = IManaitaPattern.multiply(this.base.getOutputs(), now);
        }
    }

    @Override
    public ManaitaMultiplier.Kind getKind() {
        return this.kind;
    }

    @Override
    public synchronized int getMultiplier() {
        this.refresh();
        return this.multiplier;
    }

    @Override
    public AEProcessingPattern getBase() {
        return this.base;
    }

    @Override
    public AEItemKey getDefinition() {
        return this.base.getDefinition();
    }

    @Override
    public IInput[] getInputs() {
        return this.base.getInputs();
    }

    @Override
    public synchronized GenericStack[] getOutputs() {
        this.refresh();
        return this.outputs;
    }

    @Override
    public int hashCode() {
        return this.getDefinition().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ManaitaProcessingPattern other && other.getDefinition().equals(this.getDefinition());
    }
}

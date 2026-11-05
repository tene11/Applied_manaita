package dev.manaita.appliedmanaita.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import appeng.block.AEBaseEntityBlock;

import dev.manaita.appliedmanaita.blockentity.ManaitaInstantProcessorBlockEntity;

/**
 * まな板分子精錬機・まな板分子醸造機のブロック。画面はなく、パターンプロバイダーの隣に置くだけで使う。
 * 見た目はまな板分子組立機と同じ「ガラスケース＋内側のオーク板」に、真ん中のマークを足したもの。
 * 置いたプレイヤーを AE2 の持ち主にする・壊したときに出力待ちを落とす、は AE2 の基本ブロックがやってくれる。
 */
public class ManaitaProcessorBlock extends AEBaseEntityBlock<ManaitaInstantProcessorBlockEntity> {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public ManaitaProcessorBlock(final Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(final BlockState currentState, final ManaitaInstantProcessorBlockEntity be) {
        return currentState.setValue(POWERED, be.isPowered());
    }
}

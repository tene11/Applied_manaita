package dev.manaita.appliedmanaita.block;

import dev.manaita.appliedmanaita.tile.TileManaitaMolecularBrewer;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * まな板分子醸造機のブロック。見た目はまな板分子組立機と同じガラスケースに、醸造台のマーク(マゼンタの二重四角)。
 * MEネットワークの電源が入っているときは、分子組立機と同じように光る。
 * 置き方・壊したときの動きは {@link BlockManaitaProcessor} にまとめてある。
 */
public class BlockManaitaMolecularBrewer extends BlockManaitaProcessor {

    public BlockManaitaMolecularBrewer() {
        this.setDefaultState(this.blockState.getBaseState().withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(final int meta) {
        return this.getDefaultState();
    }

    @Override
    public int getMetaFromState(final IBlockState state) {
        return 0; // 電源の状態は保存しない(タイルから読む)
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        return state.withProperty(POWERED, isPowered(world, pos));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(final World world, final int meta) {
        return new TileManaitaMolecularBrewer();
    }
}

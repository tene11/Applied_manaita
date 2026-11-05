package dev.manaita.appliedmanaita.block;

import dev.manaita.appliedmanaita.tile.TileManaitaMolecularSmelter;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * まな板分子精錬機のブロック。見た目はまな板分子組立機と同じガラスケースに、かまどのマーク(赤と黄の二重丸)。
 * MEネットワークの電源が入っているときは、分子組立機と同じように光る。向き(FACING)は見た目には使っていない。
 * 置き方・壊したときの動きは {@link BlockManaitaProcessor} にまとめてある。
 */
public class BlockManaitaMolecularSmelter extends BlockManaitaProcessor {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public BlockManaitaMolecularSmelter() {
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(final int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(final IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex(); // 電源の状態は保存しない(タイルから読む)
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        return state.withProperty(POWERED, isPowered(world, pos));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(final World world, final BlockPos pos, final EnumFacing facing, final float hitX,
                                            final float hitY, final float hitZ, final int meta, final EntityLivingBase placer) {
        // かまどと同じく、正面を置いた人の方へ向ける
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(final IBlockState state, final Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(final IBlockState state, final Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(final World world, final int meta) {
        return new TileManaitaMolecularSmelter();
    }
}

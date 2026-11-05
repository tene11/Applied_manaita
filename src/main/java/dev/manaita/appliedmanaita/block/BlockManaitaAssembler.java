package dev.manaita.appliedmanaita.block;

import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.GuiHandler;
import dev.manaita.appliedmanaita.tile.TileManaitaAssembler;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * まな板分子組立機のブロック。見た目は AE2 の分子組立機のガラスケースの内側に、オーク板を張ったもの。
 * MEネットワークの電源が入っているときは、AE2 の分子組立機と同じように光る。
 */
public class BlockManaitaAssembler extends Block implements ITileEntityProvider {

    /** 電源が入っているか。AE2の分子組立機と同じく、入っているときは光るモデルを重ねる */
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockManaitaAssembler() {
        super(Material.IRON);
        this.setHardness(2.2F);
        this.setResistance(11.0F);
        this.setSoundType(SoundType.METAL);
        this.setLightOpacity(1);
        this.setHarvestLevel("pickaxe", 0); // AE2のブロックと同じく、どのツルハシでも掘れる
        this.setDefaultState(this.blockState.getBaseState().withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERED);
    }

    @Override
    public int getMetaFromState(final IBlockState state) {
        return 0; // 電源の状態は保存しない(タイルから読む)
    }

    @Override
    public IBlockState getActualState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        final TileEntity te = world.getTileEntity(pos);
        final boolean powered = te instanceof TileManaitaAssembler && ((TileManaitaAssembler) te).isPowered();
        return state.withProperty(POWERED, powered);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(final World world, final int meta) {
        return new TileManaitaAssembler();
    }

    @Override
    public boolean onBlockActivated(final World world, final BlockPos pos, final IBlockState state, final EntityPlayer player,
                                    final EnumHand hand, final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
        if (player.isSneaking()) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(AppliedManaita.instance, GuiHandler.GUI_ASSEMBLER, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(final World world, final BlockPos pos, final IBlockState state, final EntityLivingBase placer, final ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        // AE2のセキュリティ端末用に、置いたプレイヤーを持ち主にする(AE2のブロックと同じ動き)
        final TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof TileManaitaAssembler && placer instanceof EntityPlayer) {
            ((TileManaitaAssembler) te).getProxy().setOwner((EntityPlayer) placer);
        }
    }

    @Override
    public void breakBlock(final World world, final BlockPos pos, final IBlockState state) {
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileManaitaAssembler) {
            for (final ItemStack stack : ((TileManaitaAssembler) te).getContentsForDrop()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean isOpaqueCube(final IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(final IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderInLayer(final IBlockState state, final BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.TRANSLUCENT;
    }
}

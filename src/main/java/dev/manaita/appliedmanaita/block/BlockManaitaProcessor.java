package dev.manaita.appliedmanaita.block;

import dev.manaita.appliedmanaita.tile.TileManaitaInstantProcessor;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * まな板分子精錬機・まな板分子醸造機のブロックの共通部分({@link TileManaitaInstantProcessor} を持つブロック)。
 * 画面はなく、MEインターフェースの隣に置くだけで使う。
 * <ul>
 *     <li>置いたプレイヤーを AE2 の持ち主にする(セキュリティ端末用。AE2のブロックと同じ動き)</li>
 *     <li>壊したとき、ネットワークに入れきれなかった完成品を落とす</li>
 *     <li>MEネットワークの電源が入っているかを、見た目用の {@link #POWERED} として読む</li>
 *     <li>見た目はまな板分子組立機と同じ「ガラスケース＋内側のオーク板＋マーク」なので、描画の設定も同じにする</li>
 * </ul>
 */
public abstract class BlockManaitaProcessor extends Block implements ITileEntityProvider {

    public static final PropertyBool POWERED = PropertyBool.create("powered");

    protected BlockManaitaProcessor() {
        super(Material.IRON);
        this.setHardness(2.2F);
        this.setResistance(11.0F);
        this.setSoundType(SoundType.METAL);
        this.setHarvestLevel("pickaxe", 0); // AE2のブロックと同じく、どのツルハシでも掘れる
    }

    /** その場所の機械に電源が入っているか(タイルから読む。メタデータには保存しない) */
    protected static boolean isPowered(final IBlockAccess world, final BlockPos pos) {
        final TileEntity te = world.getTileEntity(pos);
        return te instanceof TileManaitaInstantProcessor && ((TileManaitaInstantProcessor) te).isPowered();
    }

    @Override
    public void onBlockPlacedBy(final World world, final BlockPos pos, final IBlockState state, final EntityLivingBase placer, final ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        final TileEntity te = world.getTileEntity(pos);
        if (!world.isRemote && te instanceof TileManaitaInstantProcessor && placer instanceof EntityPlayer) {
            ((TileManaitaInstantProcessor) te).getProxy().setOwner((EntityPlayer) placer);
        }
    }

    @Override
    public void breakBlock(final World world, final BlockPos pos, final IBlockState state) {
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileManaitaInstantProcessor) {
            for (final ItemStack stack : ((TileManaitaInstantProcessor) te).getContentsForDrop()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.breakBlock(world, pos, state);
    }

    // ---------------------------------------------------------------- 見た目(まな板分子組立機と同じガラスケース)

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(final IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(final IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /** 稼働中の光(AE2の光るテクスチャ)は半透明なので TRANSLUCENT でも描く */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderInLayer(final IBlockState state, final BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.TRANSLUCENT;
    }
}

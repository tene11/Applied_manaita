package dev.manaita.appliedmanaita;

import dev.manaita.appliedmanaita.client.GuiManaitaAssembler;
import dev.manaita.appliedmanaita.container.ContainerManaitaAssembler;
import dev.manaita.appliedmanaita.tile.TileManaitaAssembler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * GUIを開くときの振り分け。
 * (まな板パターンターミナルの画面は AE2 のパターンターミナルの画面をそのまま使うので、ここでは扱わない)
 */
public class GuiHandler implements IGuiHandler {

    public static final int GUI_ASSEMBLER = 1;

    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        final TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == GUI_ASSEMBLER && te instanceof TileManaitaAssembler) {
            return new ContainerManaitaAssembler(player.inventory, (TileManaitaAssembler) te);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        final TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == GUI_ASSEMBLER && te instanceof TileManaitaAssembler) {
            return new GuiManaitaAssembler(new ContainerManaitaAssembler(player.inventory, (TileManaitaAssembler) te));
        }
        return null;
    }
}

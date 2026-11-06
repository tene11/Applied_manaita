package dev.manaita.appliedmanaita.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;

import dev.manaita.appliedmanaita.blockentity.ManaitaAssemblerBlockEntity;
import dev.manaita.appliedmanaita.menu.ManaitaAssemblerMenu;

/**
 * まな板分子組立機のブロック。見た目は AE2 の分子組立機のガラスケースの内側に、オーク板を張ったもの。
 * ME ネットワークの電源が入っているときは、AE2 の分子組立機と同じように光る({@link #POWERED})。
 */
public class ManaitaAssemblerBlock extends AEBaseEntityBlock<ManaitaAssemblerBlockEntity> {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public ManaitaAssemblerBlock(final Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(final BlockState currentState, final ManaitaAssemblerBlockEntity be) {
        return currentState.setValue(POWERED, be.isPowered());
    }

    @Override
    public InteractionResult onActivated(final Level level, final BlockPos pos, final Player player, final InteractionHand hand,
                                         @Nullable final ItemStack heldItem, final BlockHitResult hit) {
        final ManaitaAssemblerBlockEntity be = this.getBlockEntity(level, pos);
        if (be != null && !InteractionUtil.isInAlternateUseMode(player)) {
            if (!level.isClientSide()) {
                MenuOpener.open(ManaitaAssemblerMenu.TYPE, player, MenuLocators.forBlockEntity(be));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}

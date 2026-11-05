package dev.manaita.appliedmanaita;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import appeng.api.AECapabilities;

import dev.manaita.appliedmanaita.blockentity.ManaitaAssemblerBlockEntity;
import dev.manaita.appliedmanaita.blockentity.ManaitaInstantProcessorBlockEntity;

/**
 * ブロックエンティティの「能力」(capability)の登録。
 * <ul>
 *     <li>ME ケーブルがつながるように(AE2 のグリッドの接続点)</li>
 *     <li>パターンプロバイダーから仕事を受け取れるように(AE2 のクラフト機械)</li>
 *     <li>まな板分子組立機: ホッパーなどから素材を入れ、完成品を取り出せるように</li>
 * </ul>
 */
public final class ModCapabilities {

    private ModCapabilities() {
    }

    static void register(final RegisterCapabilitiesEvent event) {
        final BlockEntityType<ManaitaAssemblerBlockEntity> assembler = ModRegistry.MANAITA_ASSEMBLER_BE.get();
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, assembler, (be, context) -> be);
        event.registerBlockEntity(AECapabilities.CRAFTING_MACHINE, assembler, (be, context) -> be);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, assembler, ManaitaAssemblerBlockEntity::getExposedItemHandler);

        registerProcessor(event, ModRegistry.MANAITA_MOLECULAR_SMELTER_BE.get());
        registerProcessor(event, ModRegistry.MANAITA_MOLECULAR_BREWER_BE.get());
    }

    private static void registerProcessor(final RegisterCapabilitiesEvent event,
                                          final BlockEntityType<ManaitaInstantProcessorBlockEntity> type) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type, (be, context) -> be);
        event.registerBlockEntity(AECapabilities.CRAFTING_MACHINE, type, (be, context) -> be);
    }
}

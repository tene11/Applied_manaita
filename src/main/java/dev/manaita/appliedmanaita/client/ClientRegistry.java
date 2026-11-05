package dev.manaita.appliedmanaita.client;

import dev.manaita.appliedmanaita.AppliedManaita;
import dev.manaita.appliedmanaita.ModRegistry;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * クライアント側だけで行うモデル登録。
 */
@Mod.EventBusSubscriber(modid = AppliedManaita.MOD_ID, value = Side.CLIENT)
public final class ClientRegistry {

    private ClientRegistry() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(ModRegistry.MANAITA_ASSEMBLER_ITEM, 0,
                new ModelResourceLocation(ModRegistry.MANAITA_ASSEMBLER.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(ModRegistry.MANAITA_PATTERN_TERMINAL, 0,
                new ModelResourceLocation(ModRegistry.MANAITA_PATTERN_TERMINAL.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(ModRegistry.MANAITA_PATTERN, 0,
                new ModelResourceLocation(ModRegistry.MANAITA_PATTERN.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(ModRegistry.MANAITA_MOLECULAR_SMELTER_ITEM, 0,
                new ModelResourceLocation(ModRegistry.MANAITA_MOLECULAR_SMELTER.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(ModRegistry.MANAITA_MOLECULAR_BREWER_ITEM, 0,
                new ModelResourceLocation(ModRegistry.MANAITA_MOLECULAR_BREWER.getRegistryName(), "inventory"));
    }
}

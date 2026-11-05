package dev.manaita.appliedmanaita;

import appeng.core.features.IStackSrc;
import appeng.tile.AEBaseTile;
import dev.manaita.appliedmanaita.block.BlockManaitaAssembler;
import dev.manaita.appliedmanaita.block.BlockManaitaMolecularBrewer;
import dev.manaita.appliedmanaita.block.BlockManaitaMolecularSmelter;
import dev.manaita.appliedmanaita.item.ItemManaitaPattern;
import dev.manaita.appliedmanaita.part.ItemPartManaitaPatternTerminal;
import dev.manaita.appliedmanaita.recipe.RecipeManaitaPatternDecode;
import dev.manaita.appliedmanaita.tile.TileManaitaAssembler;
import dev.manaita.appliedmanaita.tile.TileManaitaMolecularBrewer;
import dev.manaita.appliedmanaita.tile.TileManaitaMolecularSmelter;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * ブロック・アイテム・TileEntity・レシピの登録をまとめたクラス。
 */
@Mod.EventBusSubscriber(modid = AppliedManaita.MOD_ID)
public final class ModRegistry {

    /** 前のバージョンにあった「まな板かまどパターン」。今は まな板パターン(かまど用)にまとめた */
    private static final ResourceLocation OLD_FURNACE_PATTERN = new ResourceLocation(AppliedManaita.MOD_ID, "manaita_furnace_pattern");

    public static final CreativeTabs TAB = new CreativeTabs(AppliedManaita.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(MANAITA_ASSEMBLER);
        }
    };

    public static BlockManaitaAssembler MANAITA_ASSEMBLER;
    public static ItemBlock MANAITA_ASSEMBLER_ITEM;
    /** まな板パターン(クラフト用・かまど用・醸造用のすべて。AE2 のエンコード済みパターンと同じく中身で分かれる) */
    public static ItemManaitaPattern MANAITA_PATTERN;
    public static BlockManaitaMolecularSmelter MANAITA_MOLECULAR_SMELTER;
    public static ItemBlock MANAITA_MOLECULAR_SMELTER_ITEM;
    public static BlockManaitaMolecularBrewer MANAITA_MOLECULAR_BREWER;
    public static ItemBlock MANAITA_MOLECULAR_BREWER_ITEM;
    /** まな板パターンターミナル(ケーブルに付けるパーツ) */
    public static ItemPartManaitaPatternTerminal MANAITA_PATTERN_TERMINAL;

    private ModRegistry() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        MANAITA_ASSEMBLER = new BlockManaitaAssembler();
        MANAITA_ASSEMBLER.setRegistryName(AppliedManaita.MOD_ID, "manaita_assembler");
        MANAITA_ASSEMBLER.setTranslationKey(AppliedManaita.MOD_ID + ".manaita_assembler");
        MANAITA_ASSEMBLER.setCreativeTab(TAB);
        event.getRegistry().register(MANAITA_ASSEMBLER);

        GameRegistry.registerTileEntity(TileManaitaAssembler.class,
                new ResourceLocation(AppliedManaita.MOD_ID, "manaita_assembler"));

        MANAITA_MOLECULAR_SMELTER = new BlockManaitaMolecularSmelter();
        MANAITA_MOLECULAR_SMELTER.setRegistryName(AppliedManaita.MOD_ID, "manaita_molecular_smelter");
        MANAITA_MOLECULAR_SMELTER.setTranslationKey(AppliedManaita.MOD_ID + ".manaita_molecular_smelter");
        MANAITA_MOLECULAR_SMELTER.setCreativeTab(TAB);
        event.getRegistry().register(MANAITA_MOLECULAR_SMELTER);

        GameRegistry.registerTileEntity(TileManaitaMolecularSmelter.class,
                new ResourceLocation(AppliedManaita.MOD_ID, "manaita_molecular_smelter"));

        MANAITA_MOLECULAR_BREWER = new BlockManaitaMolecularBrewer();
        MANAITA_MOLECULAR_BREWER.setRegistryName(AppliedManaita.MOD_ID, "manaita_molecular_brewer");
        MANAITA_MOLECULAR_BREWER.setTranslationKey(AppliedManaita.MOD_ID + ".manaita_molecular_brewer");
        MANAITA_MOLECULAR_BREWER.setCreativeTab(TAB);
        event.getRegistry().register(MANAITA_MOLECULAR_BREWER);

        GameRegistry.registerTileEntity(TileManaitaMolecularBrewer.class,
                new ResourceLocation(AppliedManaita.MOD_ID, "manaita_molecular_brewer"));

        // AE2のネットワーク画面などで「どのブロックか」を表示するために登録しておく
        registerTileItem(TileManaitaAssembler.class, MANAITA_ASSEMBLER);
        registerTileItem(TileManaitaMolecularSmelter.class, MANAITA_MOLECULAR_SMELTER);
        registerTileItem(TileManaitaMolecularBrewer.class, MANAITA_MOLECULAR_BREWER);
    }

    private static void registerTileItem(final Class<? extends AEBaseTile> tile, final Block block) {
        AEBaseTile.registerTileItem(tile, new IStackSrc() {
            @Override
            public ItemStack stack(int i) {
                return new ItemStack(block, i);
            }

            @Override
            public Item getItem() {
                return Item.getItemFromBlock(block);
            }

            @Override
            public int getDamage() {
                return 0;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        });
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        MANAITA_ASSEMBLER_ITEM = new ItemBlock(MANAITA_ASSEMBLER);
        MANAITA_ASSEMBLER_ITEM.setRegistryName(MANAITA_ASSEMBLER.getRegistryName());
        event.getRegistry().register(MANAITA_ASSEMBLER_ITEM);

        MANAITA_MOLECULAR_SMELTER_ITEM = new ItemBlock(MANAITA_MOLECULAR_SMELTER);
        MANAITA_MOLECULAR_SMELTER_ITEM.setRegistryName(MANAITA_MOLECULAR_SMELTER.getRegistryName());
        event.getRegistry().register(MANAITA_MOLECULAR_SMELTER_ITEM);

        MANAITA_MOLECULAR_BREWER_ITEM = new ItemBlock(MANAITA_MOLECULAR_BREWER);
        MANAITA_MOLECULAR_BREWER_ITEM.setRegistryName(MANAITA_MOLECULAR_BREWER.getRegistryName());
        event.getRegistry().register(MANAITA_MOLECULAR_BREWER_ITEM);

        MANAITA_PATTERN_TERMINAL = new ItemPartManaitaPatternTerminal();
        MANAITA_PATTERN_TERMINAL.setRegistryName(AppliedManaita.MOD_ID, "manaita_pattern_terminal");
        MANAITA_PATTERN_TERMINAL.setTranslationKey(AppliedManaita.MOD_ID + ".manaita_pattern_terminal");
        MANAITA_PATTERN_TERMINAL.setCreativeTab(TAB);
        event.getRegistry().register(MANAITA_PATTERN_TERMINAL);

        MANAITA_PATTERN = new ItemManaitaPattern();
        MANAITA_PATTERN.setRegistryName(AppliedManaita.MOD_ID, "manaita_pattern");
        MANAITA_PATTERN.setTranslationKey(AppliedManaita.MOD_ID + ".manaita_pattern");
        MANAITA_PATTERN.setCreativeTab(TAB);
        event.getRegistry().register(MANAITA_PATTERN);
    }

    /**
     * 前のバージョンの「まな板かまどパターン」を、まな板パターンに置き換える。
     * 中身(NBT)は AE2 の加工パターンと同じ形で crafting=false なので、そのまま かまど用のまな板パターン として使える。
     */
    @SubscribeEvent
    public static void remapItems(RegistryEvent.MissingMappings<Item> event) {
        for (final RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (OLD_FURNACE_PATTERN.equals(mapping.key)) {
                mapping.remap(MANAITA_PATTERN);
            }
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new RecipeManaitaPatternDecode()
                .setRegistryName(AppliedManaita.MOD_ID, "manaita_pattern_decode"));
    }
}

package dev.manaita.appliedmanaita;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.block.AEBaseBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.items.parts.PartItem;

import dev.manaita.appliedmanaita.block.ManaitaAssemblerBlock;
import dev.manaita.appliedmanaita.block.ManaitaProcessorBlock;
import dev.manaita.appliedmanaita.blockentity.ManaitaAssemblerBlockEntity;
import dev.manaita.appliedmanaita.blockentity.ManaitaInstantProcessorBlockEntity;
import dev.manaita.appliedmanaita.crafting.ManaitaCraftingPattern;
import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import dev.manaita.appliedmanaita.crafting.ManaitaProcessingPattern;
import dev.manaita.appliedmanaita.menu.ManaitaAssemblerMenu;
import dev.manaita.appliedmanaita.part.ManaitaPatternTerminalPart;
import dev.manaita.appliedmanaita.recipe.ManaitaPatternDecodeRecipe;

/**
 * ブロック・アイテム・ブロックエンティティ・画面・レシピの登録をまとめたクラス。
 */
public final class ModRegistry {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AppliedManaita.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AppliedManaita.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AppliedManaita.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AppliedManaita.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AppliedManaita.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AppliedManaita.MOD_ID);

    // ---------------------------------------------------------------- ブロック
    // 見た目と性質は AE2 の分子組立機と同じ(金属の音・どのツルハシでも掘れる・中が透けて見える)

    public static final DeferredBlock<ManaitaAssemblerBlock> MANAITA_ASSEMBLER = BLOCKS.register(
            "manaita_assembler", () -> new ManaitaAssemblerBlock(AEBaseBlock.metalProps().noOcclusion()));

    public static final DeferredBlock<ManaitaProcessorBlock> MANAITA_MOLECULAR_SMELTER = BLOCKS.register(
            "manaita_molecular_smelter", () -> new ManaitaProcessorBlock(AEBaseBlock.metalProps().noOcclusion()));

    public static final DeferredBlock<ManaitaProcessorBlock> MANAITA_MOLECULAR_BREWER = BLOCKS.register(
            "manaita_molecular_brewer", () -> new ManaitaProcessorBlock(AEBaseBlock.metalProps().noOcclusion()));

    // ---------------------------------------------------------------- アイテム

    public static final DeferredItem<BlockItem> MANAITA_ASSEMBLER_ITEM = ITEMS.registerSimpleBlockItem(MANAITA_ASSEMBLER);
    public static final DeferredItem<BlockItem> MANAITA_MOLECULAR_SMELTER_ITEM = ITEMS.registerSimpleBlockItem(MANAITA_MOLECULAR_SMELTER);
    public static final DeferredItem<BlockItem> MANAITA_MOLECULAR_BREWER_ITEM = ITEMS.registerSimpleBlockItem(MANAITA_MOLECULAR_BREWER);

    /** まな板パターンターミナル(ケーブルに付けるパーツ) */
    public static final DeferredItem<PartItem<ManaitaPatternTerminalPart>> MANAITA_PATTERN_TERMINAL = ITEMS.register(
            "manaita_pattern_terminal",
            () -> new PartItem<>(new Item.Properties(), ManaitaPatternTerminalPart.class, ManaitaPatternTerminalPart::new));

    /**
     * まな板クラフトパターン(AE2 のクラフトパターンと同じ中身)。出力は作業まな板の倍率倍。
     * AE2 と同じく、エンコード済みパターンのアイテムとして作る(シフトで完成品のアイコンになる、説明欄など)。
     */
    public static final DeferredItem<Item> MANAITA_CRAFTING_PATTERN = ITEMS.register(
            "manaita_crafting_pattern",
            () -> PatternDetailsHelper.encodedPatternItemBuilder(ManaitaCraftingPattern::new)
                    .invalidPatternTooltip(AECraftingPattern::getInvalidPatternTooltip)
                    .build());

    /**
     * まな板加工パターン(AE2 の加工パターンと同じ中身)。かまどレシピか醸造レシピだけが有効で、
     * 出力は調理まな板／醸造まな板の倍率倍。
     */
    public static final DeferredItem<Item> MANAITA_PROCESSING_PATTERN = ITEMS.register(
            "manaita_processing_pattern",
            () -> PatternDetailsHelper.encodedPatternItemBuilder(ManaitaProcessingPattern::new)
                    .invalidPatternTooltip(AEProcessingPattern::getInvalidPatternTooltip)
                    .build());

    // ---------------------------------------------------------------- ブロックエンティティ

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaitaAssemblerBlockEntity>> MANAITA_ASSEMBLER_BE =
            BLOCK_ENTITIES.register("manaita_assembler", () -> {
                final AtomicReference<BlockEntityType<ManaitaAssemblerBlockEntity>> holder = new AtomicReference<>();
                final BlockEntityType<ManaitaAssemblerBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new ManaitaAssemblerBlockEntity(holder.get(), pos, state), MANAITA_ASSEMBLER.get())
                        .build(null);
                holder.set(type);
                // AE2 のネットワーク画面などで、どのブロックかを表示するため
                AEBaseBlockEntity.registerBlockEntityItem(type, MANAITA_ASSEMBLER_ITEM.get());
                MANAITA_ASSEMBLER.get().setBlockEntity(ManaitaAssemblerBlockEntity.class, type, null, null);
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaitaInstantProcessorBlockEntity>> MANAITA_MOLECULAR_SMELTER_BE =
            BLOCK_ENTITIES.register("manaita_molecular_smelter",
                    instantProcessor(MANAITA_MOLECULAR_SMELTER, MANAITA_MOLECULAR_SMELTER_ITEM, ManaitaMultiplier.Kind.FURNACE));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaitaInstantProcessorBlockEntity>> MANAITA_MOLECULAR_BREWER_BE =
            BLOCK_ENTITIES.register("manaita_molecular_brewer",
                    instantProcessor(MANAITA_MOLECULAR_BREWER, MANAITA_MOLECULAR_BREWER_ITEM, ManaitaMultiplier.Kind.BREWING));

    private static Supplier<BlockEntityType<ManaitaInstantProcessorBlockEntity>> instantProcessor(
            final DeferredBlock<ManaitaProcessorBlock> block, final DeferredItem<BlockItem> item, final ManaitaMultiplier.Kind kind) {
        return () -> {
            final AtomicReference<BlockEntityType<ManaitaInstantProcessorBlockEntity>> holder = new AtomicReference<>();
            final BlockEntityType<ManaitaInstantProcessorBlockEntity> type = BlockEntityType.Builder
                    .of((pos, state) -> new ManaitaInstantProcessorBlockEntity(holder.get(), pos, state, kind), block.get())
                    .build(null);
            holder.set(type);
            AEBaseBlockEntity.registerBlockEntityItem(type, item.get());
            block.get().setBlockEntity(ManaitaInstantProcessorBlockEntity.class, type, null, null);
            return type;
        };
    }

    // ---------------------------------------------------------------- 画面

    public static final DeferredHolder<MenuType<?>, MenuType<ManaitaAssemblerMenu>> MANAITA_ASSEMBLER_MENU =
            MENUS.register("manaita_assembler", () -> ManaitaAssemblerMenu.TYPE);

    // ---------------------------------------------------------------- レシピ

    /** まな板パターンを1個だけ置くと AE2 のパターンに戻るレシピ */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ManaitaPatternDecodeRecipe>> MANAITA_PATTERN_DECODE =
            RECIPE_SERIALIZERS.register("manaita_pattern_decode",
                    () -> new SimpleCraftingRecipeSerializer<>(ManaitaPatternDecodeRecipe::new));

    // ---------------------------------------------------------------- クリエイティブタブ

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.appliedmanaita"))
                    .icon(() -> new ItemStack(MANAITA_ASSEMBLER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(MANAITA_PATTERN_TERMINAL.get());
                        output.accept(MANAITA_ASSEMBLER_ITEM.get());
                        output.accept(MANAITA_MOLECULAR_SMELTER_ITEM.get());
                        output.accept(MANAITA_MOLECULAR_BREWER_ITEM.get());
                    })
                    .build());

    private ModRegistry() {
    }

    static void register(final IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        TABS.register(modBus);
    }
}

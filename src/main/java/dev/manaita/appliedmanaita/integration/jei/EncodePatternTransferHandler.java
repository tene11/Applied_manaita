package dev.manaita.appliedmanaita.integration.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import appeng.api.stacks.GenericStack;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;

import dev.manaita.appliedmanaita.AppliedManaita;

/**
 * JEI の「+」ボタンで、レシピをパターンエンコーディングターミナルのエンコード欄に入れる。
 * <p>
 * AE2 1.21.1 は EMI 連携しか持っていないので、JEI だけの環境ではこれがないと転送できない。
 * まな板パターンターミナルは AE2 のパターンエンコーディングターミナルと同じ画面(PatternEncodingTermMenu)なので、
 * 両方のターミナルで使える。エンコード欄への入れ方は AE2 の EMI 連携と同じ処理(EncodingHelper)を使う。
 * <ul>
 *     <li>作業台・鍛冶台・石切台のレシピ → クラフト/鍛冶台/石切台モードで入れる</li>
 *     <li>それ以外(かまど・醸造台・ほかの mod の機械など) → 加工モードで、材料と完成品を入れる</li>
 * </ul>
 * まな板パターンにするかどうかは、エンコードしたとき(PatternEncodingTermMenuMixin)に決まる。
 */
public class EncodePatternTransferHandler implements IUniversalRecipeTransferHandler<PatternEncodingTermMenu> {

    private final IRecipeTransferHandlerHelper helper;

    public EncodePatternTransferHandler(final IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends PatternEncodingTermMenu> getContainerClass() {
        return PatternEncodingTermMenu.class;
    }

    @Override
    public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() {
        return Optional.of(PatternEncodingTermMenu.TYPE);
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(final PatternEncodingTermMenu menu, final Object recipe,
            final IRecipeSlotsView recipeSlots, final Player player, final boolean maxTransfer, final boolean doTransfer) {
        final RecipeHolder<?> holder = recipe instanceof RecipeHolder<?> h ? h : null;
        final boolean craftingMode = holder != null && EncodingHelper.isSupportedCraftingRecipe(holder.value());

        if (craftingMode) {
            if (holder.value() instanceof CraftingRecipe crafting && !crafting.canCraftInDimensions(3, 3)) {
                return this.helper.createUserErrorWithTooltip(ItemModText.RECIPE_TOO_LARGE.text());
            }
            if (!doTransfer) {
                return null; // 「+」ボタンを出す
            }
            try {
                // 並びはレシピから AE2 が決める。スロットの中身は予備として渡す
                EncodingHelper.encodeCraftingRecipe(menu, holder, perSlot(recipeSlots, true), stack -> true);
            } catch (final LinkageError e) {
                return this.internalError(e);
            }
            return null;
        }

        final List<List<GenericStack>> inputs = perSlot(recipeSlots, false);
        final List<GenericStack> outputs = outputs(recipeSlots);
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return this.helper.createInternalError(); // 転送できるものがない(ボタンを出さない)
        }
        if (!doTransfer) {
            return null;
        }
        try {
            EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
        } catch (final LinkageError e) {
            return this.internalError(e);
        }
        return null;
    }

    /** AE2 の中の処理が見つからなかったとき(AE2 の版が違うなど)。クラッシュさせずにログを出す */
    private IRecipeTransferError internalError(final LinkageError e) {
        AppliedManaita.LOGGER.error("[Applied Manaita] JEI からのレシピ転送に失敗しました(AE2 の版が合っていない可能性があります)", e);
        return this.helper.createInternalError();
    }

    /**
     * 材料スロットごとの候補一覧。
     * keepEmpty が true なら空のスロットも空の一覧として残す(作業台の 3x3 の並びを保つため)。
     */
    private static List<List<GenericStack>> perSlot(final IRecipeSlotsView recipeSlots, final boolean keepEmpty) {
        final List<List<GenericStack>> result = new ArrayList<>();
        for (final IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            final List<GenericStack> candidates = new ArrayList<>();
            // 表示中のものを先頭にする(同じ優先度なら先頭が選ばれる)
            slot.getDisplayedIngredient().map(EncodePatternTransferHandler::toGeneric).ifPresent(candidates::add);
            slot.getAllIngredients()
                    .map(EncodePatternTransferHandler::toGeneric)
                    .filter(Objects::nonNull)
                    .filter(stack -> !candidates.contains(stack))
                    .forEach(candidates::add);
            if (!candidates.isEmpty() || keepEmpty) {
                result.add(candidates);
            }
        }
        return result;
    }

    /** 完成品スロットの中身(各スロットで表示中のもの) */
    private static List<GenericStack> outputs(final IRecipeSlotsView recipeSlots) {
        final List<GenericStack> result = new ArrayList<>();
        for (final IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
            final Optional<ITypedIngredient<?>> displayed = slot.getDisplayedIngredient();
            final GenericStack stack = displayed.isPresent()
                    ? toGeneric(displayed.get())
                    : slot.getAllIngredients().map(EncodePatternTransferHandler::toGeneric)
                            .filter(Objects::nonNull).findFirst().orElse(null);
            if (stack != null) {
                result.add(stack);
            }
        }
        return result;
    }

    /** JEI のアイテム/液体を AE2 の GenericStack にする。それ以外の種類は null */
    private static @Nullable GenericStack toGeneric(final ITypedIngredient<?> ingredient) {
        final Optional<ItemStack> item = ingredient.getItemStack();
        if (item.isPresent()) {
            return item.get().isEmpty() ? null : GenericStack.fromItemStack(item.get());
        }
        return ingredient.getIngredient(NeoForgeTypes.FLUID_STACK)
                .filter(fluid -> !fluid.isEmpty())
                .map(GenericStack::fromFluidStack)
                .orElse(null);
    }
}

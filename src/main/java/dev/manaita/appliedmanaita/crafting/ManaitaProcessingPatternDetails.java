package dev.manaita.appliedmanaita.crafting;

import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.PatternHelper;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * 加工用のまな板パターン(crafting=false)の中身の共通部分。かまど用・醸造用がこれを継承する。
 * <p>
 * 中身は AE2 の加工パターンと同じNBTで、レシピに合っているものだけが有効({@link #create})。
 * 読み込むたびにレシピと照らし合わせるので、CraftTweaker などでレシピが変わって合わなくなったパターンは
 * 無効(読み込めないパターン)になる。比率の水増し(少ない素材で多くの完成品)も、これで防いでいる。
 */
public abstract class ManaitaProcessingPatternDetails extends AbstractManaitaPatternDetails {

    protected ManaitaProcessingPatternDetails(final ItemStack is, final PatternHelper base) {
        super(is, base);
    }

    /**
     * 加工用のまな板パターンの中身を作る。かまどレシピならかまど用、醸造レシピなら醸造用。
     * どちらにも合わなければ null(素材の種類の数が1と2で違うので、両方に合うことはない)。
     *
     * @throws RuntimeException パターンのNBTが壊れている場合(AE2 の {@link PatternHelper} が投げる)
     */
    @Nullable
    public static ManaitaProcessingPatternDetails create(final ItemStack is, final World w) {
        final NBTTagCompound tag = is.getTagCompound();
        if (tag == null || tag.getBoolean("crafting")) {
            return null;
        }
        final PatternHelper base = new PatternHelper(is, w);
        if (PatternUtil.isSmeltingPattern(base)) {
            return new ManaitaFurnacePatternDetails(is, base);
        }
        if (PatternUtil.isBrewingPattern(base)) {
            return new ManaitaBrewingPatternDetails(is, base);
        }
        return null;
    }

    /** 完成品(倍率をかける前。個数はパターンに書いた数) */
    @Override
    public ItemStack getBaseOutput() {
        final IAEItemStack[] outputs = this.base.getCondensedOutputs();
        return outputs.length > 0 && outputs[0] != null ? outputs[0].createItemStack() : ItemStack.EMPTY;
    }

    @Override
    public boolean isValidItemForSlot(final int slotIndex, final ItemStack itemStack, final World world) {
        return false; // 加工パターンなので使われない
    }

    @Override
    public boolean isCraftable() {
        // 加工パターン。普通の分子組立機・まな板分子組立機はこれを見て受け取らない
        return false;
    }

    @Override
    public boolean canSubstitute() {
        return false;
    }

    @Override
    public List<IAEItemStack> getSubstituteInputs(final int slot) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getOutput(final InventoryCrafting craftingInv, final World world) {
        return ItemStack.EMPTY; // 加工パターンなので使われない
    }
}

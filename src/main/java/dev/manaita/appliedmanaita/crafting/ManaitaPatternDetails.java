package dev.manaita.appliedmanaita.crafting;

import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.PatternHelper;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.List;

/**
 * クラフト用のまな板パターンの中身(作業台レシピ)。まな板分子組立機 が受け取る。
 * <p>
 * AE2 と同じクラフトパターンとして扱うので、代替素材なども AE2 と同じに効く。
 * 出力は まな板+ の作業まな板の倍率(craftingDoubling)倍。
 * 普通の分子組立機では使えないようにしてある(mixin/MixinTileMolecularAssembler)。
 */
public class ManaitaPatternDetails extends AbstractManaitaPatternDetails {

    /**
     * @throws IllegalArgumentException / IllegalStateException パターンが壊れている、またはクラフトパターンでない場合
     */
    public ManaitaPatternDetails(final ItemStack is, final World w) {
        super(requireCrafting(is), new PatternHelper(is, w)); // NBTを読むだけなので、まな板パターンのスタックをそのまま渡せる
    }

    private static ItemStack requireCrafting(final ItemStack is) {
        final NBTTagCompound tag = is.getTagCompound();
        if (tag == null || !tag.getBoolean("crafting")) {
            throw new IllegalArgumentException("クラフト用のまな板パターンではありません");
        }
        return is;
    }

    @Override
    protected int readMultiplier() {
        return ManaitaMultiplier.getCraftingMultiplier();
    }

    @Override
    public String getMultiplierTooltipKey() {
        return "tooltip.appliedmanaita.multiplier";
    }

    /** 1回分の完成品(倍率をかける前) */
    @Override
    public ItemStack getBaseOutput() {
        final IAEItemStack[] outputs = this.base.getOutputs();
        return outputs.length > 0 && outputs[0] != null ? outputs[0].createItemStack() : ItemStack.EMPTY;
    }

    @Override
    public boolean isValidItemForSlot(final int slotIndex, final ItemStack itemStack, final World world) {
        return this.base.isValidItemForSlot(slotIndex, itemStack, world);
    }

    @Override
    public boolean isCraftable() {
        // AE2 と同じクラフトパターンとして扱う(代替素材・ツールの耐久値・バケツなどの扱いが AE2 と同じになる)。
        // 普通の分子組立機が受け取らないようにするのは mixin/MixinTileMolecularAssembler で行う。
        return true;
    }

    @Override
    public boolean canSubstitute() {
        return this.base.canSubstitute();
    }

    @Override
    public List<IAEItemStack> getSubstituteInputs(final int slot) {
        return this.base.getSubstituteInputs(slot);
    }

    /**
     * 3x3 の素材から、倍率をかけたクラフト結果を返す。素材がレシピに合わなければ空。
     * ItemStack の個数は int なので、あふれる場合は Integer.MAX_VALUE で止める。
     * (実際に出力するときは {@link #getOutputs()} の long の数を使う)
     */
    @Override
    public ItemStack getOutput(final InventoryCrafting craftingInv, final World world) {
        final ItemStack result = this.base.getOutput(craftingInv, world);
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final long count = (long) result.getCount() * this.getMultiplier();
        result.setCount((int) Math.min(Integer.MAX_VALUE, count));
        return result;
    }
}

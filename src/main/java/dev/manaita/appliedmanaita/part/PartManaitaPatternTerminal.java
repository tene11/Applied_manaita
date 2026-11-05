package dev.manaita.appliedmanaita.part;

import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.PatternHelper;
import appeng.parts.reporting.PartPatternTerminal;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import dev.manaita.appliedmanaita.crafting.PatternUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.items.IItemHandler;

/**
 * まな板パターンターミナル(ケーブルに付けるパーツ)。
 * <p>
 * AE2 のパターンターミナルそのものを元にしているので、画面(MEのアイテム一覧・検索・JEI連携)、
 * クラフト/加工モード、代替素材、×2などのボタン、見た目は AE2 と同じ。
 * 違うのは「エンコード」で出てくるアイテムだけで、クラフトモードでは まな板パターン(クラフト用)、
 * 加工モードでかまどレシピ・醸造レシピなら まな板パターン(加工用) になる
 * (書き換えは mixin/MixinContainerPatternEncoder)。
 * AE2 のターミナルと同じく、電力とチャンネルが必要。
 */
public class PartManaitaPatternTerminal extends PartPatternTerminal {

    public PartManaitaPatternTerminal(final ItemStack is) {
        super(is);
    }

    /** 電力とチャンネルがないときは、画面を開かずにお知らせを出す */
    @Override
    public boolean onPartActivate(final EntityPlayer player, final EnumHand hand, final Vec3d pos) {
        final TileEntity te = this.getTile();
        final boolean wrench = te != null && Platform.isWrench(player, player.getHeldItem(hand), te.getPos());
        if (!wrench && te != null && !te.getWorld().isRemote && !this.getProxy().isActive()) {
            player.sendStatusMessage(new TextComponentTranslation("chat.appliedmanaita.pattern_terminal_offline"), true);
            return true;
        }
        return super.onPartActivate(player, hand, pos);
    }

    /**
     * 出口に まな板パターン(どの用途でも) を入れたとき、そのレシピを枠に読み込む。
     * AE2 本体の読み込みだと出力が倍率込みの数になってしまうので、倍率をかける前の中身で読み込む。
     */
    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc,
                                  final ItemStack removedStack, final ItemStack newStack) {
        if (inv == this.pattern && slot == 1) {
            final ItemStack is = this.pattern.getStackInSlot(1);
            if ((PatternUtil.isManaitaPattern(is) || PatternUtil.isManaitaProcessingPattern(is))
                    && is.hasTagCompound() && this.getTile() != null) {
                try {
                    final PatternHelper base = new PatternHelper(is, this.getTile().getWorld());
                    this.setCraftingRecipe(base.isCraftable());
                    this.setSubstitution(base.canSubstitute());

                    final IAEItemStack[] inputs = base.getInputs();
                    for (int x = 0; x < this.crafting.getSlots(); x++) {
                        final IAEItemStack item = x < inputs.length ? inputs[x] : null;
                        this.crafting.setStackInSlot(x, item == null ? ItemStack.EMPTY : item.createItemStack());
                    }
                    final IAEItemStack[] outputs = base.getOutputs();
                    for (int x = 0; x < this.output.getSlots(); x++) {
                        final IAEItemStack item = x < outputs.length ? outputs[x] : null;
                        this.output.setStackInSlot(x, item == null ? ItemStack.EMPTY : item.createItemStack());
                    }
                } catch (final RuntimeException e) {
                    // 壊れたパターンは読み込まない
                }
                this.getHost().markForSave();
                return;
            }
        }
        super.onChangeInventory(inv, slot, mc, removedStack, newStack);
    }
}

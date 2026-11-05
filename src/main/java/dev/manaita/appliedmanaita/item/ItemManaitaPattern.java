package dev.manaita.appliedmanaita.item;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.core.AppEng;
import appeng.items.misc.ItemEncodedPattern;
import dev.manaita.appliedmanaita.crafting.IManaitaPatternDetails;
import dev.manaita.appliedmanaita.crafting.ManaitaBrewingPatternDetails;
import dev.manaita.appliedmanaita.crafting.ManaitaFurnacePatternDetails;
import dev.manaita.appliedmanaita.crafting.ManaitaPatternDetails;
import dev.manaita.appliedmanaita.crafting.ManaitaProcessingPatternDetails;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * まな板パターン。AE2 の「エンコード済みパターン」と同じく1種類のアイテムで、
 * 中の印(NBT の {@code crafting})と中身のレシピで使い道が決まる。
 * <ul>
 *     <li>{@code crafting=true}(クラフト用): 作業台レシピ。出力が まな板+ の作業まな板の倍率倍。
 *     MEインターフェースに入れて、隣に まな板分子組立機 を置いて使う({@link ManaitaPatternDetails})</li>
 *     <li>{@code crafting=false} でかまどレシピ(かまど用): 素材1種類 → 完成品。出力が調理まな板の倍率倍。
 *     隣に まな板分子精錬機 を置いて使う({@link ManaitaFurnacePatternDetails})</li>
 *     <li>{@code crafting=false} で醸造レシピ(醸造用): 材料1個 ＋ 元のポーション1〜3本 → 完成品。
 *     出力が醸造まな板の倍率倍。隣に まな板分子醸造機 を置いて使う({@link ManaitaBrewingPatternDetails})</li>
 * </ul>
 * AE2 のエンコード済みパターン({@link ItemEncodedPattern})を継承しているので、AE2 と同じく
 * <ul>
 *     <li>説明欄は AE2 と同じ表示(クラフト用は「作成」と代替素材の有無、加工用は「生成」)。個数は倍率込み</li>
 *     <li>MEインターフェースやインターフェースターミナルの画面では、完成品のアイコンで表示される</li>
 *     <li>シフトを押しながら右クリックすると、空白パターンに戻る</li>
 * </ul>
 * シフトを押している間に完成品のアイコンになるのは、クライアント側の {@code ManaitaPatternModel} で行う。
 */
public class ItemManaitaPattern extends ItemEncodedPattern {

    /**
     * 表示用の中身のキャッシュ(クライアント側)。毎フレームのレシピ検索を避けるため。
     * キーはパターンのNBT、値は中身(無効なパターンは空)。倍率は中身が呼ばれるたびに読み直すので、キャッシュしても古くならない。
     */
    private static final Map<NBTTagCompound, Optional<IManaitaPatternDetails>> DISPLAY_CACHE = new HashMap<>();
    private static final int DISPLAY_CACHE_LIMIT = 1000;

    /** クラフト用か(中の印 {@code crafting} を見る。NBT がなければ false) */
    public static boolean isCraftingType(final ItemStack is) {
        final NBTTagCompound tag = is.getTagCompound();
        return tag != null && tag.getBoolean("crafting");
    }

    @Override
    @Nullable
    public ICraftingPatternDetails getPatternForItem(final ItemStack is, final World w) {
        try {
            return isCraftingType(is) ? new ManaitaPatternDetails(is, w) : ManaitaProcessingPatternDetails.create(is, w);
        } catch (final RuntimeException e) {
            return null; // NBT が壊れている、またはレシピに合わないパターン
        }
    }

    /**
     * 完成品のアイテム(個数は倍率込み)。AE2 の画面やアイコン表示で使われる。
     * AE2 本体のキャッシュは個数も覚えてしまうので、倍率が変わっても追従できるよう自前で計算する。
     */
    @Override
    public ItemStack getOutput(final ItemStack item) {
        final IManaitaPatternDetails details = this.getDisplayDetails(item);
        if (details == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack out = details.getBaseOutput();
        if (out.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final long count = (long) out.getCount() * details.getMultiplier();
        out.setCount((int) Math.min(Integer.MAX_VALUE, count));
        return out;
    }

    /**
     * 説明欄。中身は AE2 のエンコード済みパターンと同じ表示(無効なパターンの表示も AE2 と同じ)で、
     * 最後に今の倍率を1行足す。
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void addCheckedInformation(final ItemStack stack, final World world, final List<String> tooltip, final ITooltipFlag flag) {
        super.addCheckedInformation(stack, world, tooltip, flag);

        final IManaitaPatternDetails details = this.getDisplayDetails(stack);
        if (details != null) {
            tooltip.add(TextFormatting.GOLD + I18n.format(details.getMultiplierTooltipKey(), details.getMultiplier()));
        }
    }

    /** 表示用の中身(キャッシュから)。無効なパターンやワールドがないときは null */
    @Nullable
    private IManaitaPatternDetails getDisplayDetails(final ItemStack item) {
        final NBTTagCompound tag = item.getTagCompound();
        if (tag == null) {
            return null;
        }
        Optional<IManaitaPatternDetails> cached = DISPLAY_CACHE.get(tag);
        if (cached == null) {
            final World w = AppEng.proxy.getWorld();
            if (w == null) {
                return null;
            }
            final ICraftingPatternDetails details = this.getPatternForItem(item, w);
            cached = details instanceof IManaitaPatternDetails ? Optional.of((IManaitaPatternDetails) details) : Optional.empty();
            if (DISPLAY_CACHE.size() > DISPLAY_CACHE_LIMIT) {
                DISPLAY_CACHE.clear(); // 念のため増えすぎないようにする
            }
            DISPLAY_CACHE.put(tag.copy(), cached);
        }
        return cached.orElse(null);
    }
}

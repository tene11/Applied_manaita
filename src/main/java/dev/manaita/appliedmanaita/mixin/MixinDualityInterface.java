package dev.manaita.appliedmanaita.mixin;

import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import appeng.util.InventoryAdaptor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.manaita.appliedmanaita.MixinCheck;
import dev.manaita.appliedmanaita.crafting.IManaitaPatternDetails;
import dev.manaita.appliedmanaita.tile.TileManaitaAssembler;
import dev.manaita.appliedmanaita.tile.TileManaitaInstantProcessor;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MEインターフェースが まな板パターン の素材を、まな板の機械以外へ送らないようにする。
 * <p>
 * AE2 のインターフェースは、隣の面を順に見て「機械(ICraftingMachine)に断られたら次の面」「普通のインベントリや
 * 別ネットワークがあれば、そこへ素材を入れる」という動きをする。まな板パターンの素材がそちらへ行くと、
 * CPU は倍率倍の完成品を待ち続け、注文が終わらなくなる。
 * そこで、まな板パターンを渡している間だけ、
 * <ul>
 *     <li>普通のインベントリ・別ネットワークを「無い」ことにする</li>
 *     <li>まな板の機械(まな板分子組立機・まな板分子精錬機・まな板分子醸造機)以外の機械を「受け付けない」ことにする</li>
 * </ul>
 * 普通の AE2 のパターンの動きは変えない。
 * <p>
 * NAE2 や CrazyAE なども同じ pushPattern を書き換えるため、@Redirect だと
 * 「同じ呼び出しを先に別の mod が差し替えていて、こちらの注入が 0 件になる」→ 起動時クラッシュ、が起きる。
 * そのため、ほかの mod の書き換えと重ねがけできる MixinExtras の @WrapOperation を使う。
 * (original.call(...) で「本来の処理 or ほかの mod が差し替えた処理」がそのまま呼ばれる)
 * <p>
 * {@link MixinCheck.Marker} を付けるのは、起動時に「Mixin が効いているか」を確かめるため。
 */
@Mixin(value = DualityInterface.class, remap = false)
public abstract class MixinDualityInterface implements MixinCheck.Marker {

    /** いま渡そうとしているのが まな板パターン か(pushPattern の間だけ true) */
    @Unique
    private boolean appliedmanaita$pushingManaita;

    @Inject(method = "pushPattern", at = @At("HEAD"))
    private void appliedmanaita$beginPush(final ICraftingPatternDetails details, final InventoryCrafting table,
                                         final CallbackInfoReturnable<Boolean> cir) {
        this.appliedmanaita$pushingManaita = details instanceof IManaitaPatternDetails;
    }

    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void appliedmanaita$endPush(final ICraftingPatternDetails details, final InventoryCrafting table,
                                       final CallbackInfoReturnable<Boolean> cir) {
        this.appliedmanaita$pushingManaita = false;
    }

    /** 別ネットワーク(ストレージバス・インターフェース越し)へ送らない */
    @WrapOperation(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/tileentity/TileEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/EnumFacing;)Ljava/lang/Object;"))
    private Object appliedmanaita$hideSubnetwork(final TileEntity te, final Capability<?> capability, final EnumFacing side,
                                                 final Operation<Object> original) {
        return this.appliedmanaita$pushingManaita ? null : original.call(te, capability, side);
    }

    /** まな板の機械以外の機械へ渡さない */
    @WrapOperation(method = "pushPattern", at = @At(value = "INVOKE",
            target = "Lappeng/api/implementations/tiles/ICraftingMachine;acceptsPlans()Z"))
    private boolean appliedmanaita$onlyManaitaMachines(final ICraftingMachine machine, final Operation<Boolean> original) {
        if (this.appliedmanaita$pushingManaita
                && !(machine instanceof TileManaitaAssembler) && !(machine instanceof TileManaitaInstantProcessor)) {
            return false;
        }
        return original.call(machine);
    }

    /**
     * 普通のインベントリ(チェスト・かまど・ほかのMODの機械など)へ送らない
     * <p>
     * Fluid Craft for AE2 (ae2fc) が入っていると、ae2fc の coremod が Mixin より先に
     * InventoryAdaptor.getAdaptor の呼び出しを CoreModHooks.wrapInventory に書き換える。
     * そのため両方を注入先にして、どちらか一方に当たればよいことにする(合計 1 か所で OK)。
     */
    @WrapOperation(method = "pushPattern", require = 1, at = {
            @At(value = "INVOKE",
                    target = "Lappeng/util/InventoryAdaptor;getAdaptor(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)Lappeng/util/InventoryAdaptor;"),
            @At(value = "INVOKE",
                    target = "Lcom/glodblock/github/coremod/CoreModHooks;wrapInventory(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)Lappeng/util/InventoryAdaptor;")
    })
    private InventoryAdaptor appliedmanaita$hideInventories(final TileEntity te, final EnumFacing side,
                                                            final Operation<InventoryAdaptor> original) {
        return this.appliedmanaita$pushingManaita ? null : original.call(te, side);
    }
}

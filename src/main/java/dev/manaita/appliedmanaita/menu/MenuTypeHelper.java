package dev.manaita.appliedmanaita.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;

import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;

/**
 * AE2 の画面の仕組み(MenuOpener / MenuLocator)で開ける画面の種類を作る。
 * <p>
 * AE2 1.20.1 の MenuTypeBuilder は名前が「ae2:」になり、AE2 自身の登録に混ざってしまうので、
 * 同じことをこのmodの名前で行う(登録は ModRegistry で行う)。中身は AE2 の MenuTypeBuilder と同じ。
 */
public final class MenuTypeHelper {

    @FunctionalInterface
    public interface Factory<M extends AEBaseMenu, H> {
        M create(int containerId, Inventory playerInv, H host);
    }

    private MenuTypeHelper() {
    }

    public static <M extends AEBaseMenu, H> MenuType<M> create(final Class<H> hostClass, final Factory<M, H> factory) {
        // クライアント側: サーバーから送られた場所から、画面の持ち主(ブロックエンティティ)を探して画面を作る
        final MenuType<M> type = IForgeMenuType.create((containerId, inv, buf) -> {
            final MenuLocator locator = MenuLocators.readFromPacket(buf);
            final H host = locator.locate(inv.player, hostClass);
            if (host == null) {
                throw new IllegalStateException("画面の持ち主が見つかりません: " + locator);
            }
            final M menu = factory.create(containerId, inv, host);
            menu.setReturnedFromSubScreen(buf.readBoolean());
            return menu;
        });

        // サーバー側: MenuOpener.open(...) で開くときの処理
        MenuOpener.addOpener(type, (player, locator, fromSubMenu) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }
            final H host = locator.locate(player, hostClass);
            if (host == null) {
                return false;
            }
            final Component title = host instanceof Nameable nameable && nameable.hasCustomName()
                    ? nameable.getCustomName()
                    : Component.empty();
            final MenuProvider provider = new SimpleMenuProvider((containerId, playerInv, p) -> {
                final M menu = factory.create(containerId, playerInv, host);
                menu.setLocator(locator);
                return menu;
            }, title);
            NetworkHooks.openScreen(serverPlayer, provider, buf -> {
                MenuLocators.writeToPacket(buf, locator);
                buf.writeBoolean(fromSubMenu);
            });
            return true;
        });
        return type;
    }
}

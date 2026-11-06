package dev.manaita.appliedmanaita.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;

import dev.manaita.appliedmanaita.menu.ManaitaAssemblerMenu;

/**
 * まな板分子組立機の画面。見た目は AE2 の分子組立機の画面そのもの(画像や配置は AE2 から読み込む)で、
 * 見出しを「まな板分子組立機」にし、出口の下に作業まな板の倍率を出す。
 * 配置は {@code assets/ae2/screens/appliedmanaita/manaita_assembler.json}(AE2 の分子組立機の画面を読み込んで少し足したもの)。
 */
public class ManaitaAssemblerScreen extends UpgradeableScreen<ManaitaAssemblerMenu> {

    /** 画面の配置ファイル(AE2 の画面の仕組みで読む。場所は assets/ae2/ の下) */
    public static final String STYLE = "/screens/appliedmanaita/manaita_assembler.json";

    private final ProgressBar progressBar;

    public ManaitaAssemblerScreen(final ManaitaAssemblerMenu menu, final Inventory playerInventory,
                                  final Component title, final ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.progressBar = new ProgressBar(this.menu, style.getImage("progressBar"), ProgressBar.Direction.VERTICAL);
        this.widgets.add("progressBar", this.progressBar);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.progressBar.setFullMsg(Component.literal(this.menu.getCurrentProgress() + "%"));
        this.setTextContent("multiplier", Component.literal("×" + this.menu.getMultiplier()));
    }
}

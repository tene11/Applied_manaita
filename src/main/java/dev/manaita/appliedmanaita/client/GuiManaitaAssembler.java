package dev.manaita.appliedmanaita.client;

import dev.manaita.appliedmanaita.container.ContainerManaitaAssembler;
import dev.manaita.appliedmanaita.crafting.ManaitaMultiplier;
import dev.manaita.appliedmanaita.tile.TileManaitaAssembler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;

import static dev.manaita.appliedmanaita.container.ContainerManaitaAssembler.PROGRESS_X;
import static dev.manaita.appliedmanaita.container.ContainerManaitaAssembler.PROGRESS_Y;

/**
 * まな板分子組立機の画面。画像は AE2 の分子組立機のもの(guis/mac.png)をそのまま使う。
 * (AE2の画像は実行時にAE2から読み込むだけで、このMODには含めていない)
 */
@SideOnly(Side.CLIENT)
public class GuiManaitaAssembler extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("appliedenergistics2", "textures/guis/mac.png");

    /** mac.png の中の、進行バー(中身)の位置と大きさ */
    private static final int BAR_U = 148;
    private static final int BAR_V = 201;
    private static final int BAR_W = 6;
    private static final int BAR_H = 18;

    private final ContainerManaitaAssembler container;

    public GuiManaitaAssembler(final ContainerManaitaAssembler container) {
        super(container);
        this.container = container;
        this.xSize = 211; // 加速カード欄を含む
        this.ySize = ContainerManaitaAssembler.HEIGHT;
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        // 進行バーにマウスを乗せたら「○%」と加速カードの枚数
        if (this.isPointInRegion(PROGRESS_X, PROGRESS_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            final TileManaitaAssembler tile = this.container.getTile();
            this.drawHoveringText(Arrays.asList(
                    this.container.getProgress() + "%",
                    TextFormatting.GRAY + I18n.format("gui.appliedmanaita.speed_cards", tile.getInstalledSpeedCards())),
                    mouseX, mouseY, this.fontRenderer);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        final int x = this.guiLeft;
        final int y = this.guiTop;

        // AE2 の GuiUpgradeable と同じ描き方: 本体 + 右側の加速カード欄
        this.drawTexturedModalRect(x, y, 0, 0, 177, this.ySize);
        this.drawTexturedModalRect(x + 177, y, 177, 0, 35, 14 + TileManaitaAssembler.UPGRADE_SLOTS * 18);

        // 進行バー(下から伸びる)
        final int filled = BAR_H * this.container.getProgress() / TileManaitaAssembler.PROGRESS_MAX;
        final int diff = BAR_H - filled;
        this.drawTexturedModalRect(x + PROGRESS_X, y + PROGRESS_Y + diff, BAR_U, BAR_V + diff, BAR_W, filled);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(I18n.format("tile.appliedmanaita.manaita_assembler.name"), 8, 6, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 3, 0x404040);
        // まな板の倍率(出口の下)
        this.fontRenderer.drawString("×" + ManaitaMultiplier.getCraftingMultiplier(), 126, 70, 0xB05000);
    }
}

package dev.manaita.appliedmanaita.mixin;

import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.container.implementations.ContainerPatternEncoder;
import appeng.core.localization.GuiText;
import dev.manaita.appliedmanaita.part.PartManaitaPatternTerminal;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * パターンターミナルの画面の見出しを、まな板パターンターミナルのときだけ「まな板パターンターミナル」にする。
 * (見つからなくてもゲームが落ちないよう require = 0)
 */
@Mixin(value = GuiPatternTerm.class, remap = false)
public abstract class MixinGuiPatternTerm {

    @Shadow
    @Final
    private ContainerPatternEncoder container;

    @Redirect(method = "drawFG", require = 0, at = @At(value = "INVOKE",
            target = "Lappeng/core/localization/GuiText;getLocal()Ljava/lang/String;"))
    private String appliedmanaita$title(final GuiText text) {
        if (this.container.getPart() instanceof PartManaitaPatternTerminal) {
            return I18n.format("item.appliedmanaita.manaita_pattern_terminal.name");
        }
        return text.getLocal();
    }
}

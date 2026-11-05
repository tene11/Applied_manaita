package dev.manaita.appliedmanaita.part;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import appeng.api.parts.IPartItem;
import appeng.parts.encoding.PatternEncodingTerminalPart;

/**
 * まな板パターンターミナル(ケーブルに付けるパーツ)。
 * <p>
 * AE2 のパターンエンコーディングターミナルそのものを元にしているので、画面(ME のアイテム一覧・検索・EMI 連携)、
 * クラフト/加工モード、代替素材、見た目は AE2 と同じ。
 * 違うのは「エンコード」で出てくるアイテムだけで、クラフトモードでは まな板クラフトパターン、
 * 加工モードでかまどレシピ・醸造レシピなら まな板加工パターン になる
 * (書き換えは mixin/PatternEncodingTermMenuMixin)。それ以外は普通の AE2 のパターンになる。
 * AE2 のターミナルと同じく、電力とチャンネルが必要。
 */
public class ManaitaPatternTerminalPart extends PatternEncodingTerminalPart {

    public ManaitaPatternTerminalPart(final IPartItem<?> partItem) {
        super(partItem);
    }

    /** 電力とチャンネルがないときは、画面を開かずにお知らせを出す */
    @Override
    public boolean onUseWithoutItem(final Player player, final Vec3 pos) {
        if (!player.level().isClientSide() && !this.isActive()) {
            player.displayClientMessage(Component.translatable("chat.appliedmanaita.pattern_terminal_offline"), true);
            return true;
        }
        return super.onUseWithoutItem(player, pos);
    }

    /**
     * 画面の見出しを「まな板パターンターミナル」にする。
     * (AE2 の画面は、名前を付けたターミナルならその名前を見出しにする。名前がなければこのパーツの名前を返す)
     */
    @Override
    public boolean hasCustomName() {
        return true;
    }

    @Override
    public Component getCustomName() {
        final Component name = super.getCustomName();
        return name != null ? name : this.getPartItem().asItem().getDescription();
    }
}

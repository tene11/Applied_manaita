package dev.manaita.appliedmanaita;

import appeng.api.config.Upgrades;
import dev.manaita.appliedmanaita.network.ModNetwork;
import dev.manaita.appliedmanaita.tile.TileManaitaAssembler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Applied Manaita : まな板+ と AE2UEL を連動させるアドオン (1.12.2版)
 */
@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        dependencies = "required-after:appliedenergistics2;required-after:manaita_plus"
)
public class AppliedManaita {

    public static final String MOD_ID = Tags.MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.Instance(Tags.MOD_ID)
    public static AppliedManaita instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 倍率をサーバーからクライアントへ知らせる通信
        ModNetwork.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Mixin が効いていなければ、ここで分かりやすいメッセージを出して止める
        MixinCheck.verify();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        // 加速カードの説明欄に「まな板分子組立機: 5枚まで」と出るようにする(AE2の分子組立機と同じ枚数)
        Upgrades.SPEED.registerItem(new ItemStack(ModRegistry.MANAITA_ASSEMBLER), TileManaitaAssembler.UPGRADE_SLOTS);
    }
}

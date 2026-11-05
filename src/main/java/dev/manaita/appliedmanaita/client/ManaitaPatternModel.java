package dev.manaita.appliedmanaita.client;

import dev.manaita.appliedmanaita.AppliedManaita;
import appeng.items.misc.ItemEncodedPattern;
import dev.manaita.appliedmanaita.ModRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

/**
 * まな板パターンのアイコン。AE2 のエンコード済みパターンと同じく、シフトを押している間は完成品のアイコンになる
 * (クラフト用・かまど用・醸造用のどれも)。
 * (AE2 の ItemEncodedPatternBakedModel と同じ仕組み: アイテムごとのモデルを差し替えられる
 *  ItemOverrideList の中で、シフトが押されていたら完成品のモデルを返す)
 */
@Mod.EventBusSubscriber(modid = AppliedManaita.MOD_ID, value = Side.CLIENT)
public final class ManaitaPatternModel {

    private ManaitaPatternModel() {
    }

    static boolean isShiftKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    /** 普通のモデルを読み込んだあとで、シフト対応のモデルで包む */
    @SubscribeEvent
    public static void onModelBake(final ModelBakeEvent event) {
        wrap(event, ModRegistry.MANAITA_PATTERN);
    }

    private static void wrap(final ModelBakeEvent event, final ItemEncodedPattern item) {
        final ModelResourceLocation location = new ModelResourceLocation(item.getRegistryName(), "inventory");
        final IBakedModel original = event.getModelRegistry().getObject(location);
        if (original != null) {
            event.getModelRegistry().putObject(location, new Baked(original, item));
        }
    }

    /** 完成品のモデルに差し替えたとき、色も完成品のものを使う(革の防具や染料などの色付きアイテム用) */
    @SubscribeEvent
    public static void onItemColors(final ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
            if (isShiftKeyDown() && stack.getItem() instanceof ItemEncodedPattern) {
                final ItemStack output = ((ItemEncodedPattern) stack.getItem()).getOutput(stack);
                if (!output.isEmpty()) {
                    return Minecraft.getMinecraft().getItemColors().colorMultiplier(output, tintIndex);
                }
            }
            return 0xFFFFFF;
        }, ModRegistry.MANAITA_PATTERN);
    }

    private static final class Baked implements IBakedModel {
        private final IBakedModel base;
        private final ItemOverrideList overrides;

        Baked(final IBakedModel base, final ItemEncodedPattern item) {
            this.base = base;
            this.overrides = new ItemOverrideList(base.getOverrides().getOverrides()) {
                @Override
                public IBakedModel handleItemState(final IBakedModel originalModel, final ItemStack stack,
                                                   @Nullable final World world, @Nullable final EntityLivingBase entity) {
                    if (isShiftKeyDown()) {
                        final ItemStack output = item.getOutput(stack);
                        if (!output.isEmpty()) {
                            return Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(output, world, entity);
                        }
                    }
                    return Baked.this.base.getOverrides().handleItemState(originalModel, stack, world, entity);
                }
            };
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable final IBlockState state, @Nullable final EnumFacing side, final long rand) {
            return this.base.getQuads(state, side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return this.base.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.base.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return this.base.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return this.base.getParticleTexture();
        }

        @Override
        @SuppressWarnings("deprecation")
        public ItemCameraTransforms getItemCameraTransforms() {
            return this.base.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return this.overrides;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(final ItemCameraTransforms.TransformType type) {
            return this.base.handlePerspective(type);
        }
    }
}

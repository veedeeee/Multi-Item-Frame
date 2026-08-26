package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Placeholder renderer: draws nothing (a real per-{@link wtf.vd.multiitemframe.frame.FrameSize}
 * model/texture is a Ch.4 (assets) task). Registering this avoids NeoForge's "no renderer for
 * entity type" crash so placement/GUI logic can already be exercised in-game.
 */
public class MultiItemFrameRenderer extends EntityRenderer<MultiItemFrameEntity> {

    private static final ResourceLocation PLACEHOLDER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/item/item_frame.png");

    public MultiItemFrameRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(MultiItemFrameEntity entity) {
        return PLACEHOLDER_TEXTURE;
    }
}

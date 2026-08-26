package wtf.vd.multiitemframe.forge.frame;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.frame.FrameSize;
import wtf.vd.multiitemframe.frame.HighlightMode;

/**
 * Renders a Multi Item Frame as a stack of flat, textured quads flush against its mounting
 * face (background layer, then a single stretched border texture spanning the whole
 * {@link FrameSize} bounding box, then a per-slot highlight overlay tinted to the slot's
 * {@link DyeColor}, then the displayed item itself). See
 * {@code tools/generate_placeholder_assets.py} for the texture-layering conventions this
 * mirrors.
 */
public class MultiItemFrameRenderer extends EntityRenderer<MultiItemFrameEntity> {

    private static final ResourceLocation FRAME_TEXTURE = frameTexture("frame.png");
    private static final ResourceLocation FRAME_GLOW_TEXTURE = frameTexture("frame_glow.png");
    private static final ResourceLocation BACKGROUND_TEXTURE = frameTexture("background.png");
    private static final ResourceLocation HIGHLIGHT_FRAME_TEXTURE = frameTexture("highlight_frame.png");
    private static final ResourceLocation HIGHLIGHT_FILL_TEXTURE = frameTexture("highlight_fill.png");

    /** Small forward offset (blocks) so layers don't z-fight with the mounting block's face. */
    private static final float WALL_OFFSET = 0.4375F;
    private static final float LAYER_STEP = 0.002F;

    private final ItemRenderer itemRenderer;

    public MultiItemFrameRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    private static ResourceLocation frameTexture(String name) {
        return new ResourceLocation(MultiItemFrame.MOD_ID, "textures/entity/" + name);
    }

    @Override
    public void render(MultiItemFrameEntity entity, float yaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.translate(0.0, 0.0, -WALL_OFFSET);

        FrameSize size = entity.getFrameSize();
        float halfWidth = size.columns() / 2.0F;
        float halfHeight = size.rows() / 2.0F;
        float depth = 0.0F;

        if (entity.isBackgroundVisible()) {
            for (int slot = 0; slot < size.slotCount(); slot++) {
                int[] gridPos = size.slotPosition(slot);
                float left = -halfWidth + gridPos[0];
                float top = halfHeight - gridPos[1];
                renderQuad(poseStack, buffer, BACKGROUND_TEXTURE, left, top - 1.0F, left + 1.0F, top, depth,
                        0xFFFFFFFF, packedLight);
            }
            depth += LAYER_STEP;
        }

        ResourceLocation frameTexture = entity instanceof GlowMultiItemFrameEntity
                ? FRAME_GLOW_TEXTURE
                : FRAME_TEXTURE;
        renderQuad(poseStack, buffer, frameTexture, -halfWidth, -halfHeight, halfWidth, halfHeight, depth,
                0xFFFFFFFF, packedLight);
        depth += LAYER_STEP;

        for (int slot = 0; slot < size.slotCount(); slot++) {
            int[] gridPos = size.slotPosition(slot);
            float left = -halfWidth + gridPos[0];
            float top = halfHeight - gridPos[1];

            HighlightMode mode = entity.getHighlightMode(slot);
            if (mode != HighlightMode.NONE) {
                DyeColor color = entity.getHighlightColor(slot);
                int rgb = color != null ? color.getFireworkColor() : 0xFFFFFF;
                ResourceLocation overlay = mode == HighlightMode.FRAME
                        ? HIGHLIGHT_FRAME_TEXTURE
                        : HIGHLIGHT_FILL_TEXTURE;
                renderQuad(poseStack, buffer, overlay, left, top - 1.0F, left + 1.0F, top, depth,
                        0xFF000000 | rgb, packedLight);
            }

            ItemStack stack = entity.getItem(slot);
            if (!stack.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(left + 0.5F, top - 0.5F, depth + LAYER_STEP);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                        poseStack, buffer, entity.level(), entity.getId());
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MultiItemFrameEntity entity) {
        return entity instanceof GlowMultiItemFrameEntity ? FRAME_GLOW_TEXTURE : FRAME_TEXTURE;
    }

    /** Draws a single unit-scaled, front-facing (+Z normal) quad tinted by {@code argbColor}. */
    private static void renderQuad(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
            float x0, float y0, float x1, float y1, float z, int argbColor, int packedLight) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(texture));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float a = ((argbColor >>> 24) & 0xFF) / 255.0F;
        float r = ((argbColor >> 16) & 0xFF) / 255.0F;
        float g = ((argbColor >> 8) & 0xFF) / 255.0F;
        float b = (argbColor & 0xFF) / 255.0F;

        vertex(consumer, pose, normal, x1, y1, z, 1.0F, 0.0F, r, g, b, a, packedLight);
        vertex(consumer, pose, normal, x0, y1, z, 0.0F, 0.0F, r, g, b, a, packedLight);
        vertex(consumer, pose, normal, x0, y0, z, 0.0F, 1.0F, r, g, b, a, packedLight);
        vertex(consumer, pose, normal, x1, y0, z, 1.0F, 1.0F, r, g, b, a, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z,
            float u, float v, float r, float g, float b, float a, int packedLight) {
        consumer.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}

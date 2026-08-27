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
    private static final ResourceLocation FRAME_SIDE_TEXTURE = frameTexture("frame_side.png");
    private static final ResourceLocation FRAME_GLOW_SIDE_TEXTURE = frameTexture("frame_glow_side.png");
    private static final ResourceLocation BACKGROUND_TEXTURE = frameTexture("background.png");
    private static final ResourceLocation HIGHLIGHT_FRAME_TEXTURE = frameTexture("highlight_frame.png");
    private static final ResourceLocation HIGHLIGHT_FILL_TEXTURE = frameTexture("highlight_fill.png");

    /** Physical thickness of the frame (1px = 1/16 block), matching vanilla Item Frame's thin panel. */
    private static final float THICKNESS = 0.0625F;
    private static final float HALF_THICKNESS = THICKNESS / 2.0F;
    /** Forward step (blocks) between stacked decal layers so they don't z-fight each other.
     *  Must be large enough to stay resolvable in the depth buffer at normal viewing distances -
     *  0.002 (the original value) was too small and caused inconsistent front/back z-fighting
     *  between the frame's own front face and the background/highlight/item layers stacked on it. */
    private static final float LAYER_STEP = 0.03F;
    /** Item icons in single-slot (1x1) frames render at vanilla Item Frame scale; multi-slot frames
     *  shrink items an extra 50% so neighboring slots' items don't visually overlap. */
    private static final float ITEM_SCALE_SINGLE_SLOT = 0.5F;
    private static final float ITEM_SCALE_MULTI_SLOT = 0.25F;

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
        // No extra positional translate here: HangingEntity#recalculateBoundingBox (vanilla,
        // inherited unmodified) already stores the entity's own position flush against the
        // mounting block's face - EntityRenderDispatcher.render() translates the PoseStack there
        // before calling this method. Adding another translate on top of that (as a previous
        // version of this method did, copying ItemFrameRenderer's own translate without also
        // replicating its matching getRenderOffset() cancellation) pushes the whole frame an
        // extra ~0.47 blocks out into the room, which is why it used to render detached from
        // the wall instead of flush against it.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));

        FrameSize size = entity.getFrameSize();
        float halfWidth = size.columns() / 2.0F;
        float halfHeight = size.rows() / 2.0F;

        boolean glowing = entity instanceof GlowMultiItemFrameEntity;
        ResourceLocation frameTexture = glowing ? FRAME_GLOW_TEXTURE : FRAME_TEXTURE;
        ResourceLocation sideTexture = glowing ? FRAME_GLOW_SIDE_TEXTURE : FRAME_SIDE_TEXTURE;
        renderBox(poseStack, buffer, frameTexture, sideTexture, -halfWidth, -halfHeight, halfWidth, halfHeight,
                packedLight);

        float depth = HALF_THICKNESS + LAYER_STEP;

        if (entity.isBackgroundVisible()) {
            for (int slot = 0; slot < size.slotCount(); slot++) {
                double[] gridPos = size.slotPosition(slot);
                float left = -halfWidth + (float) gridPos[0];
                float top = halfHeight - (float) gridPos[1];
                renderQuad(poseStack, buffer, BACKGROUND_TEXTURE, left, top - 1.0F, left + 1.0F, top, depth,
                        0xFFFFFFFF, packedLight, 1.0F);
            }
            depth += LAYER_STEP;
        }

        float itemScale = size.slotCount() > 1 ? ITEM_SCALE_MULTI_SLOT : ITEM_SCALE_SINGLE_SLOT;

        for (int slot = 0; slot < size.slotCount(); slot++) {
            double[] gridPos = size.slotPosition(slot);
            float left = -halfWidth + (float) gridPos[0];
            float top = halfHeight - (float) gridPos[1];

            HighlightMode mode = entity.getHighlightMode(slot);
            DyeColor color = entity.getHighlightColor(slot);
            if (color != null) {
                int rgb = color.getFireworkColor();
                ResourceLocation overlay = mode == HighlightMode.FRAME
                        ? HIGHLIGHT_FRAME_TEXTURE
                        : HIGHLIGHT_FILL_TEXTURE;
                renderQuad(poseStack, buffer, overlay, left, top - 1.0F, left + 1.0F, top, depth,
                        0xFF000000 | rgb, packedLight, 1.0F);
            }

            ItemStack stack = entity.getItem(slot);
            if (!stack.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(left + 0.5F, top - 0.5F, depth + LAYER_STEP);
                poseStack.scale(itemScale, itemScale, itemScale);
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

    /**
     * Draws the frame's physical body as a thin box: a front face (matching the texture/depth
     * layering everything else stacks onto), a mirrored back face (so the frame isn't invisible
     * when viewed from behind, e.g. mounted on a free-standing 1-block-thick board), and 4 side
     * strips connecting them so the frame reads as having real {@link #THICKNESS}.
     */
    private static void renderBox(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation faceTexture,
            ResourceLocation sideTexture, float x0, float y0, float x1, float y1, int packedLight) {
        renderQuad(poseStack, buffer, faceTexture, x0, y0, x1, y1, HALF_THICKNESS, 0xFFFFFFFF, packedLight, 1.0F);
        renderQuad(poseStack, buffer, faceTexture, x0, y0, x1, y1, -HALF_THICKNESS, 0xFFFFFFFF, packedLight, -1.0F);

        renderSide(poseStack, buffer, sideTexture, x0, y1, x1, y1, packedLight, 0.0F, 1.0F); // top
        renderSide(poseStack, buffer, sideTexture, x1, y0, x0, y0, packedLight, 0.0F, -1.0F); // bottom
        renderSide(poseStack, buffer, sideTexture, x0, y0, x0, y1, packedLight, -1.0F, 0.0F); // left
        renderSide(poseStack, buffer, sideTexture, x1, y1, x1, y0, packedLight, 1.0F, 0.0F); // right
    }

    /**
     * Draws one thin edge strip of the frame's box between its front ({@code +HALF_THICKNESS})
     * and back ({@code -HALF_THICKNESS}) faces, from {@code (ax, ay)} to {@code (bx, by)} (given
     * in the winding order that faces {@code (nx, ny, 0)} outward).
     */
    private static void renderSide(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
            float ax, float ay, float bx, float by, int packedLight, float nx, float ny) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(texture));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        vertex(consumer, pose, normal, bx, by, -HALF_THICKNESS, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, nx, ny, 0.0F, packedLight);
        vertex(consumer, pose, normal, bx, by, HALF_THICKNESS, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, nx, ny, 0.0F, packedLight);
        vertex(consumer, pose, normal, ax, ay, HALF_THICKNESS, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, nx, ny, 0.0F, packedLight);
        vertex(consumer, pose, normal, ax, ay, -HALF_THICKNESS, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, nx, ny, 0.0F, packedLight);
    }

    /**
     * Draws a single unit-scaled quad tinted by {@code argbColor}, facing {@code +Z} when
     * {@code normalZ > 0} (visible looking toward {@code -Z}) or facing {@code -Z} when
     * {@code normalZ < 0} (visible looking toward {@code +Z}) - used to give the frame's box a
     * back face without it being culled the same as its front face.
     */
    private static void renderQuad(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
            float x0, float y0, float x1, float y1, float z, int argbColor, int packedLight, float normalZ) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(texture));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float a = ((argbColor >>> 24) & 0xFF) / 255.0F;
        float r = ((argbColor >> 16) & 0xFF) / 255.0F;
        float g = ((argbColor >> 8) & 0xFF) / 255.0F;
        float b = (argbColor & 0xFF) / 255.0F;

        if (normalZ > 0) {
            vertex(consumer, pose, normal, x1, y1, z, 1.0F, 0.0F, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x0, y1, z, 0.0F, 0.0F, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x0, y0, z, 0.0F, 1.0F, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x1, y0, z, 1.0F, 1.0F, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
        } else {
            vertex(consumer, pose, normal, x1, y0, z, 1.0F, 1.0F, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x0, y0, z, 0.0F, 1.0F, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x0, y1, z, 0.0F, 0.0F, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x1, y1, z, 1.0F, 0.0F, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z,
            float u, float v, float r, float g, float b, float a, float nx, float ny, float nz, int packedLight) {
        consumer.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}

package wtf.vd.multiitemframe.neoforge.frame;

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
     *  Kept as small as possible while still avoiding depth-buffer precision issues: the previous
     *  0.03 value (needed back when content stacked toward the frame's front face) made the
     *  background/highlight/item layers visibly float off the back face at grazing/side viewing
     *  angles now that they stack toward it instead. */
    private static final float LAYER_STEP = 0.004F;
    /** Item icons render at vanilla Item Frame scale (0.5) within a full 1x1 cell; multi-slot
     *  frames additionally shrink by each cell's own share of the single-block frame (see
     *  {@code render()}) so items never overflow their smaller cell. */
    private static final float ITEM_SCALE_SINGLE_SLOT = 0.5F;

    private final ItemRenderer itemRenderer;

    public MultiItemFrameRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    private static ResourceLocation frameTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiItemFrame.MOD_ID, "textures/entity/" + name);
    }

    @Override
    public ResourceLocation getTextureLocation(MultiItemFrameEntity entity) {
        return entity instanceof GlowMultiItemFrameEntity ? FRAME_GLOW_TEXTURE : FRAME_TEXTURE;
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
        // The frame's physical footprint is always exactly 1x1 block (matching its 1x1 collision
        // box - see MultiItemFrameEntity#calculateBoundingBox()); FrameSize.columns()/rows() only
        // subdivide that single block face into a grid of smaller cells, they never make the
        // rendered frame itself larger than one block.
        final float halfWidth = 0.5F;
        final float halfHeight = 0.5F;
        float cellWidth = 1.0F / size.columns();
        float cellHeight = 1.0F / size.rows();

        boolean glowing = entity instanceof GlowMultiItemFrameEntity;
        ResourceLocation frameTexture = glowing ? FRAME_GLOW_TEXTURE : FRAME_TEXTURE;
        ResourceLocation sideTexture = glowing ? FRAME_GLOW_SIDE_TEXTURE : FRAME_SIDE_TEXTURE;
        renderBox(poseStack, buffer, frameTexture, sideTexture, -halfWidth, -halfHeight, halfWidth, halfHeight,
                packedLight);

        // Background/highlight/item layers stack toward -Z rather than +Z: empirical in-game
        // testing showed items/highlights only appeared on the far side of the mounting block
        // (visible "through" transparent blocks like glass) and were invisible from the actual
        // accessible/open side where the GUI is opened - i.e. the opposite of what the +Z
        // convention documented in ItemFrameRenderer (decompiled) would suggest for this entity.
        // Rather than re-deriving the exact winding/axis reason (previous attempts at that kind
        // of reasoning didn't match real behavior), the layers are simply stacked toward -Z,
        // which was verified to put them on the correct/open side.
        float depth = -(HALF_THICKNESS + LAYER_STEP);

        if (entity.isBackgroundVisible()) {
            for (int slot = 0; slot < size.slotCount(); slot++) {
                double[] gridPos = size.slotPosition(slot);
                // Mirrored horizontally (see the comment on the item/highlight loop below for why).
                float left = halfWidth - (float) gridPos[0] * cellWidth - cellWidth;
                float top = halfHeight - (float) gridPos[1] * cellHeight;
                renderQuad(poseStack, buffer, BACKGROUND_TEXTURE, left, top - cellHeight, left + cellWidth, top,
                        depth, 0xFFFFFFFF, packedLight, -1.0F);
            }
            depth -= LAYER_STEP;
        }

        // Items keep vanilla Item Frame's scale (0.5) within a full 1x1 cell (single-slot frames);
        // multi-slot frames additionally shrink by each cell's own share of the block so items
        // never overflow their (now smaller) cell.
        float itemScale = ITEM_SCALE_SINGLE_SLOT * Math.min(cellWidth, cellHeight);

        for (int slot = 0; slot < size.slotCount(); slot++) {
            double[] gridPos = size.slotPosition(slot);
            // The X axis is mirrored (right-to-left instead of left-to-right) compared to
            // gridPos[0]'s literal value: turning the -Z sign flip (see depth's comment above)
            // means the viewer on the accessible side is now facing the opposite way along Z
            // from what these local coordinates were originally authored for, which mirrors their
            // apparent left/right (but not up/down - Y is unaffected by a turn-around along Z).
            // Without this, slot 0 (top-left in the GUI, per FrameSize#slotPosition) would render
            // on the viewer's right instead of their left, out of sync with the settings GUI.
            float left = halfWidth - (float) gridPos[0] * cellWidth - cellWidth;
            float top = halfHeight - (float) gridPos[1] * cellHeight;

            HighlightMode mode = entity.getHighlightMode(slot);
            DyeColor color = entity.getHighlightColor(slot);
            if (color != null) {
                int rgb = color.getFireworkColor();
                if (mode == HighlightMode.FRAME) {
                    renderHighlightFrameBorder(poseStack, buffer, left, top - cellHeight, left + cellWidth, top,
                            depth, 0xFF000000 | rgb, packedLight);
                } else {
                    renderQuad(poseStack, buffer, HIGHLIGHT_FILL_TEXTURE, left, top - cellHeight, left + cellWidth,
                            top, depth, 0xFF000000 | rgb, packedLight, -1.0F);
                }
            }

            ItemStack stack = entity.getItem(slot);
            if (!stack.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(left + cellWidth / 2.0F, top - cellHeight / 2.0F, depth - LAYER_STEP);
                poseStack.scale(itemScale, itemScale, itemScale);
                this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                        poseStack, buffer, entity.level(), entity.getId());
                poseStack.popPose();
            }
        }

        poseStack.popPose();
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
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, bx, by, -HALF_THICKNESS, 1.0F, 1.0F, 255, 255, 255, 255, nx, ny, 0.0F, packedLight);
        vertex(consumer, pose, bx, by, HALF_THICKNESS, 1.0F, 0.0F, 255, 255, 255, 255, nx, ny, 0.0F, packedLight);
        vertex(consumer, pose, ax, ay, HALF_THICKNESS, 0.0F, 0.0F, 255, 255, 255, 255, nx, ny, 0.0F, packedLight);
        vertex(consumer, pose, ax, ay, -HALF_THICKNESS, 0.0F, 1.0F, 255, 255, 255, 255, nx, ny, 0.0F, packedLight);
    }

    /**
     * Draws a single unit-scaled quad tinted by {@code argbColor}, facing {@code +Z} when
     * {@code normalZ > 0} (visible looking toward {@code -Z}) or facing {@code -Z} when
     * {@code normalZ < 0} (visible looking toward {@code +Z}) - used to give the frame's box a
     * back face without it being culled the same as its front face.
     */
    private static void renderQuad(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
            float x0, float y0, float x1, float y1, float z, int argbColor, int packedLight, float normalZ) {
        renderQuad(poseStack, buffer, texture, x0, y0, x1, y1, z, 0.0F, 0.0F, 1.0F, 1.0F, argbColor, packedLight,
                normalZ);
    }

    /**
     * Same as {@link #renderQuad(PoseStack, MultiBufferSource, ResourceLocation, float, float, float, float, float,
     * int, int, float)} but with explicit texture UV bounds, so a quad can sample a sub-region of its texture
     * instead of always stretching the whole thing across it (used to draw fixed-pixel-width highlight borders -
     * see {@link #renderHighlightFrameBorder}).
     */
    private static void renderQuad(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture,
            float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1, int argbColor,
            int packedLight, float normalZ) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        int a = (argbColor >>> 24) & 0xFF;
        int r = (argbColor >> 16) & 0xFF;
        int g = (argbColor >> 8) & 0xFF;
        int b = argbColor & 0xFF;

        if (normalZ > 0) {
            vertex(consumer, pose, x1, y1, z, u1, v0, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, x0, y1, z, u0, v0, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, x0, y0, z, u0, v1, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, x1, y0, z, u1, v1, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
        } else {
            vertex(consumer, pose, x1, y0, z, u1, v1, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, x0, y0, z, u0, v1, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, x0, y1, z, u0, v0, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, x1, y1, z, u1, v0, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
        }
    }

    /** Physical width (blocks) of each highlight-frame border stroke; 1px, matching {@link #THICKNESS}'s unit. */
    private static final float HIGHLIGHT_BORDER_PX = 1.0F / 16.0F;
    /** UV fraction of {@link #HIGHLIGHT_FRAME_TEXTURE} guaranteed to be solid border color (its outer 2 of 16
     *  texels on every edge) - sampling only this corner lets every border stroke be a fixed physical pixel
     *  width regardless of the cell's aspect ratio. */
    private static final float HIGHLIGHT_FRAME_SOLID_UV = 0.125F;

    /**
     * Draws {@link #HIGHLIGHT_FRAME_TEXTURE}'s border as 4 separate strips, each a fixed
     * {@link #HIGHLIGHT_BORDER_PX} thick, instead of stretching a single quad across the whole cell. Stretching
     * a single quad made the texture's border scale anisotropically on non-square cells (e.g. 1x2/2x1 frames),
     * ending up 2px thick on the cell's short edges and 1px on its long edges instead of a uniform 1px everywhere.
     */
    private static void renderHighlightFrameBorder(PoseStack poseStack, MultiBufferSource buffer, float x0, float y0,
            float x1, float y1, float z, int argbColor, int packedLight) {
        float t = HIGHLIGHT_BORDER_PX;
        float uv = HIGHLIGHT_FRAME_SOLID_UV;
        renderQuad(poseStack, buffer, HIGHLIGHT_FRAME_TEXTURE, x0, y1 - t, x1, y1, z, 0.0F, 0.0F, uv, uv, argbColor,
                packedLight, -1.0F); // top
        renderQuad(poseStack, buffer, HIGHLIGHT_FRAME_TEXTURE, x0, y0, x1, y0 + t, z, 0.0F, 0.0F, uv, uv, argbColor,
                packedLight, -1.0F); // bottom
        renderQuad(poseStack, buffer, HIGHLIGHT_FRAME_TEXTURE, x0, y0 + t, x0 + t, y1 - t, z, 0.0F, 0.0F, uv, uv,
                argbColor, packedLight, -1.0F); // left
        renderQuad(poseStack, buffer, HIGHLIGHT_FRAME_TEXTURE, x1 - t, y0 + t, x1, y1 - t, z, 0.0F, 0.0F, uv, uv,
                argbColor, packedLight, -1.0F); // right
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int r, int g, int b, int a, float nx, float ny, float nz, int packedLight) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }
}

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
import net.minecraft.core.BlockPos;
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
    private static final ResourceLocation FRAME_SIDE_TEXTURE = frameTexture("frame_side.png");
    private static final ResourceLocation BACKGROUND_TEXTURE = frameTexture("background.png");
    private static final ResourceLocation HIGHLIGHT_FRAME_TEXTURE = frameTexture("highlight_frame.png");
    private static final ResourceLocation HIGHLIGHT_FILL_TEXTURE = frameTexture("highlight_fill.png");

    /** Physical thickness of the frame (1px = 1/16 block), matching vanilla Item Frame's thin panel. */
    private static final float THICKNESS = 0.0625F;
    private static final float HALF_THICKNESS = THICKNESS / 2.0F;
    /** Item icons render at vanilla Item Frame scale (0.5) within a full 1x1 cell; multi-slot
     *  frames additionally shrink by each cell's own share of the single-block frame (see
     *  {@code render()}) so items never overflow their smaller cell. */
    private static final float ITEM_SCALE_SINGLE_SLOT = 0.5F;
    /** Visual footprint ratio (matching vanilla Item Frame's real model: a 12x12 border+backing
     *  panel within its 16x16 block face, i.e. a 2px margin on each side) applied to the frame's
     *  box and all content layers. The entity's actual placement/collision footprint stays a full
     *  1x1 block (see {@code getWidth()}/{@code getHeight()}/{@code calculateBoundingBox()}) -
     *  only the rendered size shrinks, exactly like vanilla's frame is visually smaller than the
     *  block it's mounted on. */
    private static final float FOOTPRINT_HALF = 0.5F * (12.0F / 16.0F);
    /** Forced minimum block light level for the glow variant, mirroring vanilla's Glow Item
     *  Frame ({@code ItemFrameRenderer#GLOW_FRAME_BRIGHTNESS}, which uses 5) but brighter per
     *  user request. Glow/non-glow otherwise share identical textures and sounds - this is the
     *  only remaining functional difference between the two variants. */
    private static final int GLOW_LIGHT_LEVEL = 9;

    private final ItemRenderer itemRenderer;

    public MultiItemFrameRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    private static ResourceLocation frameTexture(String name) {
        return new ResourceLocation(MultiItemFrame.MOD_ID, "textures/entity/" + name);
    }

    @Override
    protected int getBlockLightLevel(MultiItemFrameEntity entity, BlockPos pos) {
        return entity instanceof GlowMultiItemFrameEntity
                ? Math.max(GLOW_LIGHT_LEVEL, super.getBlockLightLevel(entity, pos))
                : super.getBlockLightLevel(entity, pos);
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
        // box - see MultiItemFrameEntity#getWidth()/getHeight()); FrameSize.columns()/rows() only
        // subdivide that single block face into a grid of smaller cells, they never make the
        // rendered frame itself larger than one block.
        final float halfWidth = FOOTPRINT_HALF;
        final float halfHeight = FOOTPRINT_HALF;
        // Cell size as a fraction of the *shrunk* footprint (halfWidth/halfHeight * 2), not of a
        // full 1.0-block face: using 1.0F / columns() here (as if the frame were still a full
        // block wide) made every content layer (background/highlight/item) sized/positioned for
        // the pre-shrink footprint, so slots - and even a single 1x1 frame's own full-cell content -
        // rendered up to (1.0 - 12/16) = 0.25 block wider/taller than the frame's own box,
        // overflowing past its edges on every side.
        float baseCellWidth = (halfWidth * 2.0F) / size.columns();
        float baseCellHeight = (halfHeight * 2.0F) / size.rows();

        // Glow and non-glow variants share identical textures - the only difference is
        // GLOW_LIGHT_LEVEL forced via getBlockLightLevel() above.
        renderBox(poseStack, buffer, FRAME_TEXTURE, FRAME_SIDE_TEXTURE, -halfWidth, -halfHeight, halfWidth,
                halfHeight, packedLight);

        // Background/highlight/item layers stack toward -Z rather than +Z: empirical in-game
        // testing showed items/highlights only appeared on the far side of the mounting block
        // (visible "through" transparent blocks like glass) and were invisible from the actual
        // accessible/open side where the GUI is opened - i.e. the opposite of what the +Z
        // convention documented in ItemFrameRenderer (decompiled) would suggest for this entity.
        // Rather than re-deriving the exact winding/axis reason (previous attempts at that kind
        // of reasoning didn't match real behavior), the layers are simply stacked toward -Z,
        // which was verified to put them on the correct/open side.
        //
        // All content layers (background/highlight/item) are drawn at the exact same depth as the
        // frame's own back face (-HALF_THICKNESS) rather than offset further behind it: an earlier
        // version pushed each layer back by a small LAYER_STEP to dodge z-fighting, but any nonzero
        // offset is a real 3D gap that reads as visibly detached, floating panels at grazing/side
        // viewing angles. RenderType.entityCutoutNoCull uses a LEQUAL depth test, so coincident
        // quads drawn in back-to-front order (background, then highlight, then item) simply
        // overdraw each other with no gap and no z-fighting flicker.
        float depth = -HALF_THICKNESS;

        if (entity.isBackgroundVisible()) {
            for (int slot = 0; slot < size.slotCount(); slot++) {
                float[] bounds = slotBounds(size, slot, halfWidth, halfHeight, baseCellWidth, baseCellHeight);
                float left = bounds[0];
                float top = bounds[1];
                float cellW = bounds[2];
                float cellH = bounds[3];
                renderQuad(poseStack, buffer, BACKGROUND_TEXTURE, left, top - cellH, left + cellW, top,
                        depth, 0xFFFFFFFF, packedLight, -1.0F);
            }
        }

        for (int slot = 0; slot < size.slotCount(); slot++) {
            float[] bounds = slotBounds(size, slot, halfWidth, halfHeight, baseCellWidth, baseCellHeight);
            float left = bounds[0];
            float top = bounds[1];
            float cellW = bounds[2];
            float cellH = bounds[3];

            HighlightMode mode = entity.getHighlightMode(slot);
            DyeColor color = entity.getHighlightColor(slot);
            if (color != null) {
                int rgb = color.getFireworkColor();
                if (mode == HighlightMode.FRAME) {
                    renderHighlightFrameBorder(poseStack, buffer, left, top - cellH, left + cellW, top,
                            depth, 0xFF000000 | rgb, packedLight);
                } else {
                    renderQuad(poseStack, buffer, HIGHLIGHT_FILL_TEXTURE, left, top - cellH, left + cellW,
                            top, depth, 0xFF000000 | rgb, packedLight, -1.0F);
                }
            }

            ItemStack stack = entity.getItem(slot);
            if (!stack.isEmpty()) {
                // Items keep vanilla Item Frame's scale (0.5) within a full 1x1 cell (single-slot
                // frames); multi-slot frames additionally shrink by each cell's own share of the
                // block so items never overflow their (now smaller, or full-width/height for a
                // lone slot - see slotBounds) cell.
                float itemScale = ITEM_SCALE_SINGLE_SLOT * Math.min(cellW, cellH);
                poseStack.pushPose();
                poseStack.translate(left + cellW / 2.0F, top - cellH / 2.0F, depth);
                poseStack.scale(itemScale, itemScale, itemScale);
                this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                        poseStack, buffer, entity.level(), entity.getId());
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    /**
     * Computes {@code {left, top, width, height}} in local block-space for a slot's content
     * layers (background/highlight/item). Most slots occupy a single {@code baseCellWidth} x
     * {@code baseCellHeight} cell positioned by {@link FrameSize#slotPosition}'s integer grid
     * coordinate, mirrored horizontally to compensate for the accessible-side viewer's flipped X
     * axis (see the comment on {@code depth} in {@link #render}: turning the -Z sign flip around
     * means their apparent left/right is mirrored relative to gridPos[0]'s literal value, but not
     * their up/down). A lone slot spanning the whole width or height of the frame (marked by a
     * fractional {@code 0.5} grid coordinate on that axis - see {@link FrameSize#ONE_AND_TWO}/
     * {@link FrameSize#TWO_AND_ONE}) instead spans the frame's full {@code halfWidth}/{@code
     * halfHeight} * 2 on that axis, rather than being centered within a single, narrower cell.
     */
    private static float[] slotBounds(FrameSize size, int slot, float halfWidth, float halfHeight,
            float baseCellWidth, float baseCellHeight) {
        double[] gridPos = size.slotPosition(slot);
        boolean fullWidth = gridPos[0] != Math.floor(gridPos[0]);
        boolean fullHeight = gridPos[1] != Math.floor(gridPos[1]);
        float cellW = fullWidth ? halfWidth * 2.0F : baseCellWidth;
        float cellH = fullHeight ? halfHeight * 2.0F : baseCellHeight;
        float right = fullWidth ? halfWidth : halfWidth - (float) gridPos[0] * baseCellWidth;
        float left = right - cellW;
        float top = fullHeight ? halfHeight : halfHeight - (float) gridPos[1] * baseCellHeight;
        return new float[] { left, top, cellW, cellH };
    }

    @Override
    public ResourceLocation getTextureLocation(MultiItemFrameEntity entity) {
        // Glow and non-glow share the same texture (see GLOW_LIGHT_LEVEL comment above).
        return FRAME_TEXTURE;
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
        renderQuad(poseStack, buffer, texture, x0, y0, x1, y1, z, 0.0F, 0.0F, 1.0F, 1.0F, argbColor, packedLight, normalZ);
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
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float a = ((argbColor >>> 24) & 0xFF) / 255.0F;
        float r = ((argbColor >> 16) & 0xFF) / 255.0F;
        float g = ((argbColor >> 8) & 0xFF) / 255.0F;
        float b = (argbColor & 0xFF) / 255.0F;

        if (normalZ > 0) {
            vertex(consumer, pose, normal, x1, y1, z, u1, v0, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x0, y1, z, u0, v0, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x0, y0, z, u0, v1, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x1, y0, z, u1, v1, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
        } else {
            vertex(consumer, pose, normal, x1, y0, z, u1, v1, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x0, y0, z, u0, v1, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x0, y1, z, u0, v0, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
            vertex(consumer, pose, normal, x1, y1, z, u1, v0, r, g, b, a, 0.0F, 0.0F, normalZ, packedLight);
        }
    }

    /** Physical width (blocks) of each highlight-frame border stroke; 1px, matching {@link #THICKNESS}'s unit. */
    private static final float HIGHLIGHT_BORDER_PX = 1.0F / 16.0F;
    /** UV fraction of {@link #HIGHLIGHT_FRAME_TEXTURE} guaranteed to be solid border color (its outer 1 of 12
     *  texels on every edge, matching the 12x12 texture's 1px border) - sampling only this corner lets every
     *  border stroke be a fixed physical pixel width regardless of the cell's aspect ratio. */
    private static final float HIGHLIGHT_FRAME_SOLID_UV = 1.0F / 12.0F;

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

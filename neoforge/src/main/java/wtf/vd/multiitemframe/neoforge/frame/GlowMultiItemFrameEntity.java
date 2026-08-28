package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Glowing variant: identical behaviour, sounds and textures to the regular frame, but actually
 *  emits real light into the surrounding room. Vanilla's Glow Item Frame does NOT do this (its
 *  {@code ItemFrameRenderer#getBlockLightLevel} override only brightens the frame's own
 *  rendering, since entities cannot contribute to the world's block-light engine - only blocks
 *  can). To get real room illumination, this places an invisible {@link Blocks#LIGHT} block at
 *  the frame's own occupied cell (the same air cell vanilla ItemFrame entities exist at) once
 *  added to the world, and removes it again once the frame is actually destroyed (not merely
 *  unloaded/dimension-changed - see {@link #remove}). */
public class GlowMultiItemFrameEntity extends MultiItemFrameEntity {

    /** Kept separate from {@code MultiItemFrameRenderer#GLOW_LIGHT_LEVEL} since the renderer is
     *  client-only and this class also runs on the server; both are set to the same value. */
    private static final int LIGHT_LEVEL = 9;

    public GlowMultiItemFrameEntity(EntityType<? extends MultiItemFrameEntity> entityType, Level level) {
        super(entityType, level);
    }

    public GlowMultiItemFrameEntity(EntityType<? extends MultiItemFrameEntity> entityType, Level level, BlockPos pos,
            Direction direction, wtf.vd.multiitemframe.frame.FrameSize frameSize) {
        super(entityType, level, pos, direction, frameSize);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        // Only claim the cell if it's currently empty air: if it's water, another entity, or
        // (shouldn't normally happen) something solid, leave it alone rather than clobbering it -
        // the frame just won't emit real light in that case, same as if placement were denied.
        if (!this.level().isClientSide && this.pos != null && this.level().getBlockState(this.pos).isAir()) {
            this.level().setBlockAndUpdate(this.pos,
                    Blocks.LIGHT.defaultBlockState().setValue(BlockStateProperties.LEVEL, LIGHT_LEVEL));
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // shouldDestroy() is false for chunk-unload/dimension-change reasons: the frame is still
        // logically "placed" in those cases (it'll be re-added later), so the light block must
        // stay. Only revert on genuine destruction (broken, /kill, etc.), and only if the cell
        // still holds the light block we placed (not something a player placed afterward).
        if (!this.level().isClientSide && reason.shouldDestroy() && this.pos != null
                && this.level().getBlockState(this.pos).is(Blocks.LIGHT)) {
            this.level().setBlockAndUpdate(this.pos, Blocks.AIR.defaultBlockState());
        }
        super.remove(reason);
    }
}

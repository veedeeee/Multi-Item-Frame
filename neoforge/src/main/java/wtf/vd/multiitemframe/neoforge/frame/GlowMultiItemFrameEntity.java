package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Glowing variant: identical behaviour, only the placement/interaction sounds differ (visual glow is a renderer concern). */
public class GlowMultiItemFrameEntity extends MultiItemFrameEntity {

    public GlowMultiItemFrameEntity(EntityType<? extends MultiItemFrameEntity> entityType, Level level) {
        super(entityType, level);
    }

    public GlowMultiItemFrameEntity(EntityType<? extends MultiItemFrameEntity> entityType, Level level, BlockPos pos,
            Direction direction, wtf.vd.multiitemframe.frame.FrameSize frameSize) {
        super(entityType, level, pos, direction, frameSize);
    }

    @Override
    public SoundEvent getPlaceSound() {
        return SoundEvents.GLOW_ITEM_FRAME_PLACE;
    }

    @Override
    public SoundEvent getBreakSound() {
        return SoundEvents.GLOW_ITEM_FRAME_BREAK;
    }
}

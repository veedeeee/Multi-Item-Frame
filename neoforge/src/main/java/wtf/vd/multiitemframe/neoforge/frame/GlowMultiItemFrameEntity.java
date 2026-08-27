package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Glowing variant: identical behaviour, sounds and textures to the regular frame - the only
 *  actual difference is the forced minimum light level applied in
 *  {@code MultiItemFrameRenderer#getBlockLightLevel}. */
public class GlowMultiItemFrameEntity extends MultiItemFrameEntity {

    public GlowMultiItemFrameEntity(EntityType<? extends MultiItemFrameEntity> entityType, Level level) {
        super(entityType, level);
    }

    public GlowMultiItemFrameEntity(EntityType<? extends MultiItemFrameEntity> entityType, Level level, BlockPos pos,
            Direction direction, wtf.vd.multiitemframe.frame.FrameSize frameSize) {
        super(entityType, level, pos, direction, frameSize);
    }
}

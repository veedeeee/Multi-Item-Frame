package wtf.vd.multiitemframe.forge.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import wtf.vd.multiitemframe.frame.FrameSize;

/**
 * Placement item for a Multi Item Frame variant.
 * Mirrors vanilla {@code HangingEntityItem#useOn}, which is hardcoded to
 * vanilla painting/item-frame entity types and therefore cannot be reused
 * directly for a custom entity type.
 */
public class MultiItemFrameItem extends Item {

    private final EntityType<? extends MultiItemFrameEntity> entityType;
    private final FrameSize frameSize;
    private final boolean glowing;

    public MultiItemFrameItem(EntityType<? extends MultiItemFrameEntity> entityType, FrameSize frameSize,
            boolean glowing, Item.Properties properties) {
        super(properties);
        this.entityType = entityType;
        this.frameSize = frameSize;
        this.glowing = glowing;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos targetPos = clickedPos.relative(face);
        Player player = context.getPlayer();
        ItemStack itemInHand = context.getItemInHand();
        if (player != null && (face.getAxis().isVertical() || !player.mayUseItemAt(targetPos, face, itemInHand))) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        MultiItemFrameEntity frame = this.glowing
                ? new GlowMultiItemFrameEntity(this.entityType, level, targetPos, face, this.frameSize)
                : new MultiItemFrameEntity(this.entityType, level, targetPos, face, this.frameSize);

        if (!frame.survives()) {
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide) {
            frame.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, frame.position());
            level.addFreshEntity(frame);
        }
        itemInHand.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

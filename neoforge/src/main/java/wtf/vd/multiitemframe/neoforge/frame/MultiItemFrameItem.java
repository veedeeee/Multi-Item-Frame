package wtf.vd.multiitemframe.neoforge.frame;

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

import java.util.function.Supplier;

/**
 * Placement item for a Multi Item Frame variant.
 * Mirrors vanilla {@code HangingEntityItem#useOn}, which is hardcoded to
 * vanilla painting/item-frame entity types and therefore cannot be reused
 * directly for a custom entity type.
 *
 * <p>The entity type is taken as a {@link Supplier} (a {@code DeferredHolder}) rather than an
 * already-resolved {@code EntityType}, because items and entity types register in independent
 * {@code RegisterEvent} passes with no guaranteed ordering between them; resolving eagerly at
 * item-registration time can throw before the entity type's holder is bound. Resolution is
 * deferred to {@link #useOn}, well after both registries have finished.
 */
public class MultiItemFrameItem extends Item {

    private final Supplier<? extends EntityType<? extends MultiItemFrameEntity>> entityType;
    private final FrameSize frameSize;
    private final boolean glowing;

    public MultiItemFrameItem(Supplier<? extends EntityType<? extends MultiItemFrameEntity>> entityType, FrameSize frameSize,
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
        EntityType<? extends MultiItemFrameEntity> resolvedType = this.entityType.get();
        MultiItemFrameEntity frame = this.glowing
                ? new GlowMultiItemFrameEntity(resolvedType, level, targetPos, face, this.frameSize)
                : new MultiItemFrameEntity(resolvedType, level, targetPos, face, this.frameSize);

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

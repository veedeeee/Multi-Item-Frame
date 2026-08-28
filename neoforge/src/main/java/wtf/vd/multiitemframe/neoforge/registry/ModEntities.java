package wtf.vd.multiitemframe.neoforge.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.neoforge.frame.GlowMultiItemFrameEntity;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameEntity;

/** Entity type registrations. Only 2 types exist: the frame layout (see FrameSize) is synced entity data, not a separate type. */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MultiItemFrame.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MultiItemFrameEntity>> MULTI_ITEM_FRAME =
            ENTITY_TYPES.register(MultiItemFrame.ENTITY_FRAME, () -> EntityType.Builder
                    .<MultiItemFrameEntity>of(MultiItemFrameEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(10)
                    .build(MultiItemFrame.ENTITY_FRAME));

    public static final DeferredHolder<EntityType<?>, EntityType<GlowMultiItemFrameEntity>> GLOW_MULTI_ITEM_FRAME =
            ENTITY_TYPES.register(MultiItemFrame.ENTITY_GLOW_FRAME, () -> EntityType.Builder
                    .<GlowMultiItemFrameEntity>of(GlowMultiItemFrameEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(10)
                    .build(MultiItemFrame.ENTITY_GLOW_FRAME));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}

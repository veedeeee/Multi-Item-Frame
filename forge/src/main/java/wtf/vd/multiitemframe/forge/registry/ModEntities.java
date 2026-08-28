package wtf.vd.multiitemframe.forge.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.forge.frame.GlowMultiItemFrameEntity;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameEntity;

/** Entity type registrations. Only 2 types exist: the frame layout (see FrameSize) is synced entity data, not a separate type. */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MultiItemFrame.MOD_ID);

    public static final RegistryObject<EntityType<MultiItemFrameEntity>> MULTI_ITEM_FRAME =
            ENTITY_TYPES.register(MultiItemFrame.ENTITY_FRAME, () -> EntityType.Builder
                    .<MultiItemFrameEntity>of(MultiItemFrameEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(10)
                    .build(MultiItemFrame.ENTITY_FRAME));

    public static final RegistryObject<EntityType<GlowMultiItemFrameEntity>> GLOW_MULTI_ITEM_FRAME =
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

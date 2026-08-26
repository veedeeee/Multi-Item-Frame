package wtf.vd.multiitemframe.forge.registry;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.frame.FrameSize;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameItem;

/** Placement item registrations: one non-glow and one glow item per {@link FrameSize}. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MultiItemFrame.MOD_ID);

    private static final Map<FrameSize, RegistryObject<Item>> FRAME_ITEMS = new EnumMap<>(FrameSize.class);
    private static final Map<FrameSize, RegistryObject<Item>> GLOW_FRAME_ITEMS = new EnumMap<>(FrameSize.class);

    static {
        for (FrameSize size : FrameSize.values()) {
            FRAME_ITEMS.put(size, ITEMS.register(MultiItemFrame.frameItemId(size),
                    () -> new MultiItemFrameItem(ModEntities.MULTI_ITEM_FRAME.get(), size, false, new Item.Properties())));
            GLOW_FRAME_ITEMS.put(size, ITEMS.register(MultiItemFrame.glowFrameItemId(size),
                    () -> new MultiItemFrameItem(ModEntities.GLOW_MULTI_ITEM_FRAME.get(), size, true, new Item.Properties())));
        }
    }

    private ModItems() {
    }

    public static Item frameItem(FrameSize size) {
        return FRAME_ITEMS.get(size).get();
    }

    public static Item glowFrameItem(FrameSize size) {
        return GLOW_FRAME_ITEMS.get(size).get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}

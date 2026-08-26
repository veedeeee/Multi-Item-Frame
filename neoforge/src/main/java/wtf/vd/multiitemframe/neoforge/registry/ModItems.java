package wtf.vd.multiitemframe.neoforge.registry;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.frame.FrameSize;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameItem;

/** Placement item registrations: one non-glow and one glow item per {@link FrameSize}. */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MultiItemFrame.MOD_ID);

    private static final Map<FrameSize, DeferredHolder<Item, Item>> FRAME_ITEMS = new EnumMap<>(FrameSize.class);
    private static final Map<FrameSize, DeferredHolder<Item, Item>> GLOW_FRAME_ITEMS = new EnumMap<>(FrameSize.class);

    static {
        for (FrameSize size : FrameSize.values()) {
            FRAME_ITEMS.put(size, ITEMS.registerItem(MultiItemFrame.frameItemId(size),
                    properties -> new MultiItemFrameItem(ModEntities.MULTI_ITEM_FRAME.get(), size, false, properties)));
            GLOW_FRAME_ITEMS.put(size, ITEMS.registerItem(MultiItemFrame.glowFrameItemId(size),
                    properties -> new MultiItemFrameItem(ModEntities.GLOW_MULTI_ITEM_FRAME.get(), size, true, properties)));
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

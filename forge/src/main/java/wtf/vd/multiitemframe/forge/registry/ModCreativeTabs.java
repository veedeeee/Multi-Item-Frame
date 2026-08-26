package wtf.vd.multiitemframe.forge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.frame.FrameSize;

/**
 * Dedicated creative tab for the mod. Without this, the items belong to no tab and are
 * invisible both in the creative inventory and in JEI (which builds its ingredient list from
 * creative tab contents, not the raw item registry, since 1.20).
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MultiItemFrame.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.multiitemframe.main"))
            .icon(() -> new ItemStack(ModItems.frameItem(FrameSize.TWO_BY_TWO)))
            .displayItems((parameters, output) -> {
                for (FrameSize size : FrameSize.values()) {
                    output.accept(ModItems.frameItem(size));
                }
                for (FrameSize size : FrameSize.values()) {
                    output.accept(ModItems.glowFrameItem(size));
                }
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
    }
}

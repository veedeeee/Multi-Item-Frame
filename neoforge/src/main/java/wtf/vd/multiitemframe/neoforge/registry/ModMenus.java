package wtf.vd.multiitemframe.neoforge.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameMenu;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, MultiItemFrame.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MultiItemFrameMenu>> MULTI_ITEM_FRAME_MENU =
            MENU_TYPES.register("multi_item_frame", () -> IMenuTypeExtension.create(MultiItemFrameMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}

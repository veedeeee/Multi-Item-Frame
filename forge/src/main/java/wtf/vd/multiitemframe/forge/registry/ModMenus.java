package wtf.vd.multiitemframe.forge.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameMenu;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MultiItemFrame.MOD_ID);

    public static final RegistryObject<MenuType<MultiItemFrameMenu>> MULTI_ITEM_FRAME_MENU =
            MENU_TYPES.register("multi_item_frame", () -> IForgeMenuType.create(MultiItemFrameMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}

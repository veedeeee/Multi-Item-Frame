package wtf.vd.multiitemframe.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameRenderer;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameScreen;
import wtf.vd.multiitemframe.forge.registry.ModCreativeTabs;
import wtf.vd.multiitemframe.forge.registry.ModEntities;
import wtf.vd.multiitemframe.forge.registry.ModItems;
import wtf.vd.multiitemframe.forge.registry.ModMenus;

@Mod(MultiItemFrame.MOD_ID)
public class MultiItemFrameForge {

    public MultiItemFrameForge() {
        MultiItemFrame.init();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterRenderers);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.MULTI_ITEM_FRAME_MENU.get(), MultiItemFrameScreen::new));
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MULTI_ITEM_FRAME.get(), MultiItemFrameRenderer::new);
        event.registerEntityRenderer(ModEntities.GLOW_MULTI_ITEM_FRAME.get(), MultiItemFrameRenderer::new);
    }
}

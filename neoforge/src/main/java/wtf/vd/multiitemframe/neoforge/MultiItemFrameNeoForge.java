package wtf.vd.multiitemframe.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameRenderer;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameScreen;
import wtf.vd.multiitemframe.neoforge.registry.ModEntities;
import wtf.vd.multiitemframe.neoforge.registry.ModItems;
import wtf.vd.multiitemframe.neoforge.registry.ModMenus;

@Mod(MultiItemFrame.MOD_ID)
public class MultiItemFrameNeoForge {

    public MultiItemFrameNeoForge(IEventBus modEventBus, ModContainer container) {
        MultiItemFrame.init();

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterRenderers);
        modEventBus.addListener(this::onRegisterMenuScreens);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
    }

    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MULTI_ITEM_FRAME_MENU.get(), MultiItemFrameScreen::new);
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MULTI_ITEM_FRAME.get(), MultiItemFrameRenderer::new);
        event.registerEntityRenderer(ModEntities.GLOW_MULTI_ITEM_FRAME.get(), MultiItemFrameRenderer::new);
    }
}

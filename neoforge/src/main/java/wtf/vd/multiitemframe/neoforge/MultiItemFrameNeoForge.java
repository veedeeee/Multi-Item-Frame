package wtf.vd.multiitemframe.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import wtf.vd.multiitemframe.MultiItemFrame;

@Mod(MultiItemFrame.MOD_ID)
public class MultiItemFrameNeoForge {

    public MultiItemFrameNeoForge(IEventBus ignoredEventBus, ModContainer container) {
        MultiItemFrame.init();
    }
}

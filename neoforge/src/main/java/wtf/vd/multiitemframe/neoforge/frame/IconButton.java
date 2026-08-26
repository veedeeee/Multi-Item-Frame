package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wtf.vd.multiitemframe.MultiItemFrame;

import java.util.function.Supplier;

/**
 * A compact 16x16 icon-only button, used for the per-slot highlight-mode and highlight-color
 * controls in {@link MultiItemFrameScreen} (matches {@code gui_sample.html}'s "item stack" row
 * of small icon buttons rather than vanilla's text buttons).
 */
public class IconButton extends Button {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(MultiItemFrame.MOD_ID, "gui/button_background.png");
    private static final ResourceLocation BACKGROUND_PRESSING =
            ResourceLocation.fromNamespaceAndPath(MultiItemFrame.MOD_ID, "gui/button_background_pressing.png");

    private final Supplier<ResourceLocation> iconSupplier;

    public IconButton(int x, int y, Supplier<ResourceLocation> iconSupplier, OnPress onPress) {
        super(x, y, 16, 16, Component.empty(), onPress, DEFAULT_NARRATION);
        this.iconSupplier = iconSupplier;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation background = this.isHoveredOrFocused() ? BACKGROUND_PRESSING : BACKGROUND;
        guiGraphics.blit(background, this.getX(), this.getY(), 0, 0, 16, 16, 16, 16);
        guiGraphics.blit(this.iconSupplier.get(), this.getX(), this.getY(), 0, 0, 16, 16, 16, 16);
    }
}

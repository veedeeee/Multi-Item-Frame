package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wtf.vd.multiitemframe.MultiItemFrame;

import java.util.function.Supplier;

/**
 * A compact 18x18 icon-only button (an 18x18 background with a 16x16 icon centered inside,
 * matching {@code gui/button_stack.png}'s layout), used for the per-slot highlight-color control in
 * {@link MultiItemFrameScreen} (matches {@code gui_sample.html}'s "item stack" row of small icon
 * buttons rather than vanilla's text buttons). Shows the "pressing" background only while the
 * mouse is actually held down on it (not just hovered), and supports an optional middle-click
 * action (used to reset the color to "no highlight"/transparent) plus a dynamic tooltip whose
 * text can depend on the button's current state (e.g. the highlight-mode icon's current mode).
 */
public class IconButton extends Button {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(MultiItemFrame.MOD_ID, "gui/button_background.png");
    private static final ResourceLocation BACKGROUND_PRESSING =
            ResourceLocation.fromNamespaceAndPath(MultiItemFrame.MOD_ID, "gui/button_background_pressing.png");

    private final Supplier<ResourceLocation> iconSupplier;
    private final Runnable onMiddleClick;
    private final Supplier<Component> tooltipSupplier;
    private boolean pressed;

    public IconButton(int x, int y, Supplier<ResourceLocation> iconSupplier, OnPress onPress) {
        this(x, y, iconSupplier, onPress, null, null);
    }

    public IconButton(int x, int y, Supplier<ResourceLocation> iconSupplier, OnPress onPress,
            Runnable onMiddleClick, Supplier<Component> tooltipSupplier) {
        super(x, y, 18, 18, Component.empty(), onPress, DEFAULT_NARRATION);
        this.iconSupplier = iconSupplier;
        this.onMiddleClick = onMiddleClick;
        this.tooltipSupplier = tooltipSupplier;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2 && this.onMiddleClick != null && this.active && this.visible
                && this.clicked(mouseX, mouseY)) {
            this.onMiddleClick.run();
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            this.pressed = true;
        }
        return handled;
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        this.pressed = false;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.tooltipSupplier != null) {
            this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(this.tooltipSupplier.get()));
        }
        ResourceLocation background = this.pressed ? BACKGROUND_PRESSING : BACKGROUND;
        guiGraphics.blit(background, this.getX(), this.getY(), 0, 0, 18, 18, 18, 18);
        guiGraphics.blit(this.iconSupplier.get(), this.getX() + 1, this.getY() + 1, 0, 0, 16, 16, 16, 16);
    }
}


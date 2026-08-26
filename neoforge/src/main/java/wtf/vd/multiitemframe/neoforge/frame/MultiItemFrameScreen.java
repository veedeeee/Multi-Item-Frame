package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import wtf.vd.multiitemframe.frame.FrameSize;

/**
 * Placeholder screen for {@link MultiItemFrameMenu}: reuses vanilla's generic container
 * background sprite (no dedicated texture yet, see TASKS.md ch.4) and adds one mode-toggle
 * and one color-toggle button per active slot, plus a single background-visibility toggle.
 */
public class MultiItemFrameScreen extends AbstractContainerScreen<MultiItemFrameMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public MultiItemFrameScreen(MultiItemFrameMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.leftPos;
        int top = this.topPos;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.multiitemframe.toggle_background"),
                        button -> this.clickButton(0))
                .bounds(left + 8, top + 16, 48, 16)
                .build());

        for (int slot = 0; slot < this.menu.slotCount; slot++) {
            int buttonX = left + 60 + (slot % 2) * 56;
            int buttonY = top + 40 + (slot / 2) * 20;
            int modeId = 1 + slot;
            int colorId = 1 + FrameSize.MAX_SLOTS + slot;
            this.addRenderableWidget(Button.builder(Component.translatable("gui.multiitemframe.mode"),
                            button -> this.clickButton(modeId))
                    .bounds(buttonX, buttonY, 26, 16)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.multiitemframe.color"),
                            button -> this.clickButton(colorId))
                    .bounds(buttonX + 28, buttonY, 26, 16)
                    .build());
        }
    }

    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}

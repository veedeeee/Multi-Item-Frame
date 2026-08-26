package wtf.vd.multiitemframe.forge.frame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import wtf.vd.multiitemframe.frame.FrameSize;

/**
 * Screen for {@link MultiItemFrameMenu}: reuses vanilla's generic container background sprite
 * (no dedicated texture yet, see TASKS.md ch.4). Per-slot controls (highlight-mode toggle,
 * highlight-color cycle) are laid out as a compact row immediately to the right of each frame
 * slot, mirroring {@code gui_sample.html}'s per-slot "item stack" widget group.
 */
public class MultiItemFrameScreen extends AbstractContainerScreen<MultiItemFrameMenu> {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    /** Gap (px) between the item slot and the mode button, and between the mode and color buttons. */
    private static final int BUTTON_GAP = 2;
    private static final int BUTTON_SIZE = 16;
    /** Item slots are vanilla's usual 18px; center the 16px-tall buttons within that row height. */
    private static final int BUTTON_Y_OFFSET = 1;

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
                .bounds(left + (this.imageWidth - 90) / 2, top + 4, 90, 16)
                .build());

        for (int slot = 0; slot < this.menu.slotCount; slot++) {
            Slot itemSlot = this.menu.getSlot(slot);
            int modeButtonX = left + itemSlot.x + 18 + BUTTON_GAP;
            int colorButtonX = modeButtonX + BUTTON_SIZE + BUTTON_GAP;
            int buttonY = top + itemSlot.y + BUTTON_Y_OFFSET;
            int slotIndex = slot;
            int modeId = 1 + slot;
            int colorId = 1 + FrameSize.MAX_SLOTS + slot;
            this.addRenderableWidget(new IconButton(modeButtonX, buttonY,
                    () -> modeIcon(this.menu.getHighlightMode(slotIndex)), button -> this.clickButton(modeId)));
            this.addRenderableWidget(new IconButton(colorButtonX, buttonY,
                    () -> colorIcon(this.menu.getHighlightColor(slotIndex)), button -> this.clickButton(colorId)));
        }
    }

    private static ResourceLocation modeIcon(wtf.vd.multiitemframe.frame.HighlightMode mode) {
        String name = mode == wtf.vd.multiitemframe.frame.HighlightMode.FRAME ? "frame" : "fill";
        return new ResourceLocation(wtf.vd.multiitemframe.MultiItemFrame.MOD_ID, "gui/highlight_mode_button_" + name + ".png");
    }

    private static ResourceLocation colorIcon(net.minecraft.world.item.DyeColor color) {
        String name = color == null ? "transparent" : color.getName();
        return new ResourceLocation(wtf.vd.multiitemframe.MultiItemFrame.MOD_ID, "gui/color_picker_button_" + name + ".png");
    }

    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    /** Number of active frame slots (used by JEI's dye-drag ghost handler to enumerate targets). */
    public int getSlotCount() {
        return this.menu.slotCount;
    }

    /** Screen-space bounds of the color-toggle button for the given frame slot. */
    public net.minecraft.client.renderer.Rect2i getColorButtonArea(int slot) {
        Slot itemSlot = this.menu.getSlot(slot);
        int modeButtonX = this.leftPos + itemSlot.x + 18 + BUTTON_GAP;
        int colorButtonX = modeButtonX + BUTTON_SIZE + BUTTON_GAP;
        int buttonY = this.topPos + itemSlot.y + BUTTON_Y_OFFSET;
        return new net.minecraft.client.renderer.Rect2i(colorButtonX, buttonY, BUTTON_SIZE, BUTTON_SIZE);
    }

    /** Sets a slot's highlight color directly (used by JEI's dye-drag ghost ingredient handler). */
    public void sendDirectColor(int slot, net.minecraft.world.item.DyeColor color) {
        this.clickButton(MultiItemFrameMenu.DIRECT_COLOR_BASE + slot * 16 + color.getId());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}

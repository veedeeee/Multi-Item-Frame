package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import wtf.vd.multiitemframe.frame.HighlightMode;

/**
 * Screen for {@link MultiItemFrameMenu}. Background/border is drawn from {@code gui/base.png}
 * (a single flat bordered panel texture, stretched to the panel's actual size). Per-slot
 * controls (highlight-mode toggle, highlight-color cycle) are laid out as a compact row
 * immediately to the right of each frame slot, mirroring {@code gui_sample.html}'s per-slot
 * "item stack" widget group.
 */
public class MultiItemFrameScreen extends AbstractContainerScreen<MultiItemFrameMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(wtf.vd.multiitemframe.MultiItemFrame.MOD_ID, "gui/base.png");

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

        for (int slot = 0; slot < this.menu.slotCount; slot++) {
            Rect2i modeArea = this.getModeButtonArea(slot);
            Rect2i colorArea = this.getColorButtonArea(slot);
            int slotIndex = slot;
            this.addRenderableWidget(new IconButton(modeArea.getX(), modeArea.getY(),
                    () -> modeIcon(this.menu.getHighlightMode(slotIndex)),
                    button -> this.clickButton(slotIndex),
                    null,
                    () -> modeTooltip(this.menu.getHighlightMode(slotIndex))));
            this.addRenderableWidget(new IconButton(colorArea.getX(), colorArea.getY(),
                    () -> colorIcon(this.menu.getHighlightColor(slotIndex)),
                    button -> this.cycleColor(slotIndex),
                    () -> this.clearColor(slotIndex),
                    () -> Component.translatable("gui.multiitemframe.color_tooltip")));
        }
    }

    private static ResourceLocation modeIcon(HighlightMode mode) {
        String name = mode == HighlightMode.FRAME ? "frame" : "filled";
        return ResourceLocation.fromNamespaceAndPath(wtf.vd.multiitemframe.MultiItemFrame.MOD_ID,
                "gui/highlight_type_button_" + name + ".png");
    }

    private static Component modeTooltip(HighlightMode mode) {
        return mode == HighlightMode.FRAME
                ? Component.translatable("gui.multiitemframe.highlight_type_frame_tooltip")
                : Component.translatable("gui.multiitemframe.highlight_type_fill_tooltip");
    }

    private static ResourceLocation colorIcon(DyeColor color) {
        String name = color == null ? "transparent" : color.getName();
        return ResourceLocation.fromNamespaceAndPath(wtf.vd.multiitemframe.MultiItemFrame.MOD_ID,
                "gui/color_picker_button_" + name + ".png");
    }

    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void cycleColor(int slot) {
        this.clickButton(MultiItemFrameMenu.COLOR_CYCLE_BASE + slot);
    }

    private void clearColor(int slot) {
        this.sendDirectColor(slot, null);
    }

    /** Number of active frame slots (used by JEI's ghost ingredient handler to enumerate targets). */
    public int getSlotCount() {
        return this.menu.slotCount;
    }

    /** Screen-space bounds of the item slot for the given frame slot (used by JEI's ghost item handler). */
    public Rect2i getItemSlotArea(int slot) {
        Slot itemSlot = this.menu.getSlot(slot);
        return new Rect2i(this.leftPos + itemSlot.x, this.topPos + itemSlot.y, BUTTON_SIZE, BUTTON_SIZE);
    }

    /** Screen-space bounds of the highlight-mode button for the given frame slot. */
    public Rect2i getModeButtonArea(int slot) {
        Slot itemSlot = this.menu.getSlot(slot);
        int modeButtonX = this.leftPos + itemSlot.x + 18 + BUTTON_GAP;
        int buttonY = this.topPos + itemSlot.y + BUTTON_Y_OFFSET;
        return new Rect2i(modeButtonX, buttonY, BUTTON_SIZE, BUTTON_SIZE);
    }

    /** Screen-space bounds of the color-toggle button for the given frame slot. */
    public Rect2i getColorButtonArea(int slot) {
        Rect2i modeArea = this.getModeButtonArea(slot);
        return new Rect2i(modeArea.getX() + BUTTON_SIZE + BUTTON_GAP, modeArea.getY(), BUTTON_SIZE, BUTTON_SIZE);
    }

    /** Sets a slot's highlight color directly (used by JEI's dye-drag ghost ingredient handler, and the color button's middle-click). */
    public void sendDirectColor(int slot, DyeColor color) {
        int value = color == null ? DyeColor.values().length : color.getId();
        this.clickButton(MultiItemFrameMenu.DIRECT_COLOR_BASE + slot * MultiItemFrameMenu.COLOR_ID_SPACE + value);
    }

    /** Sets a slot's displayed item directly (used by JEI's item-drag ghost ingredient handler). */
    public void sendDirectItem(int slot, ItemStack stack) {
        int itemId = stack.isEmpty() ? BuiltInRegistries.ITEM.getId(Items.AIR) : BuiltInRegistries.ITEM.getId(stack.getItem());
        this.clickButton(MultiItemFrameMenu.DIRECT_ITEM_BASE + slot * MultiItemFrameMenu.ITEM_ID_SPACE + itemId);
    }

    /**
     * Middle-click clears a frame's item slot regardless of game mode (vanilla only reaches
     * {@code ClickType.CLONE} for middle-click in Creative, so we forward it to the menu
     * ourselves here the same way any other slot click is forwarded).
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2) {
            for (int slot = 0; slot < this.menu.slotCount; slot++) {
                Slot itemSlot = this.menu.getSlot(slot);
                if (this.isHovering(itemSlot.x, itemSlot.y, 16, 16, mouseX, mouseY)) {
                    this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, itemSlot.index, button,
                            ClickType.PICKUP, this.minecraft.player);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int slot = 0; slot < this.menu.slotCount; slot++) {
            if (this.getItemSlotArea(slot).contains(mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, Component.translatable("gui.multiitemframe.item_slot_tooltip"), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0, 0, 256, 256, 256, 256);
    }
}

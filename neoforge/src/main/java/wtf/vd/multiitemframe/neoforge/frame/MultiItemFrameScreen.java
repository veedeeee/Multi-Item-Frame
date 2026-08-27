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
 * Screen for {@link MultiItemFrameMenu}. Background/border is drawn from
 * {@code gui/main_gui_background.png} (176x166 panel with a transparent settings viewport and
 * a vanilla-shaped player inventory area baked in); each frame slot's item-slot background comes
 * from {@code gui/item_slot_background.png}, drawn separately since the viewport itself is
 * transparent. Per-slot controls (highlight-mode toggle, highlight-color cycle) are laid out as
 * a compact row immediately to the right of each frame slot, mirroring {@code gui_sample.html}'s
 * per-slot "item stack" widget group (see {@code gui/button_stack.png} for the reference layout).
 */
public class MultiItemFrameScreen extends AbstractContainerScreen<MultiItemFrameMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(wtf.vd.multiitemframe.MultiItemFrame.MOD_ID, "gui/main_gui_background.png");
    private static final ResourceLocation ITEM_SLOT_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(wtf.vd.multiitemframe.MultiItemFrame.MOD_ID, "gui/item_slot_background.png");
    /** {@code main_gui_background.png}'s settings viewport is a transparent rectangular hole (its
     *  own border/bevel is already baked into the surrounding artwork, including the panel's
     *  rounded top corners) - pixel-measured bounds of that hole within the 176x166 panel, used
     *  to backstop it with a flat fill so the game world doesn't show through. Filling only this
     *  rect (rather than the whole panel) avoids covering/squaring off the artwork's rounded
     *  corners with a solid rectangle. Fill color (198,198,198) matches the border/bevel tone
     *  sampled directly from the same texture, so there's no visible seam between the backstop
     *  and the surrounding artwork. */
    private static final int VIEWPORT_LEFT = 7;
    private static final int VIEWPORT_TOP = 8;
    private static final int VIEWPORT_WIDTH = 162;
    private static final int VIEWPORT_HEIGHT = 71;
    private static final int VIEWPORT_FILL_COLOR = 0xFFC6C6C6;
    /** Grid divider line color/thickness, matching the {@code gui/main_gui_*_placeholder.png}
     *  reference layouts' 1px mid-gray lines (sampled: RGB 135,135,135). */
    private static final int GRID_DIVIDER_COLOR = 0xFF878787;
    private static final int GRID_DIVIDER_THICKNESS = 1;

    /** Gap (px) between the item slot and the mode button, and between the mode and color buttons. */
    private static final int BUTTON_GAP = 3;
    private static final int BUTTON_SIZE = 18;
    /** Item slots are vanilla's usual 16px icon, in an 18px-wide cell (same width as a button). */
    private static final int ITEM_SLOT_ICON_SIZE = 16;
    private static final int ITEM_SLOT_CELL_SIZE = 18;
    /** {@code item_slot_background.png} is a full 18x18 cell (matching the mode/color buttons'
     *  size), drawn 1px up/left of the 16x16 icon position so the icon sits centered within it -
     *  the same convention vanilla inventory slots use. */
    private static final int ITEM_SLOT_BG_SIZE = 18;
    private static final int ITEM_SLOT_BG_INSET = 1;

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
        return new Rect2i(this.leftPos + itemSlot.x, this.topPos + itemSlot.y, ITEM_SLOT_ICON_SIZE, ITEM_SLOT_ICON_SIZE);
    }

    /** Screen-space bounds of the highlight-mode button for the given frame slot. */
    public Rect2i getModeButtonArea(int slot) {
        Slot itemSlot = this.menu.getSlot(slot);
        int modeButtonX = this.leftPos + itemSlot.x + ITEM_SLOT_CELL_SIZE + BUTTON_GAP;
        int buttonY = this.topPos + itemSlot.y;
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
                if (this.isHovering(itemSlot.x, itemSlot.y, ITEM_SLOT_ICON_SIZE, ITEM_SLOT_ICON_SIZE, mouseX, mouseY)) {
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
        // Backstop the settings viewport's transparent hole with a flat fill matching the
        // surrounding artwork's tone, so the game world doesn't show through it - see the
        // VIEWPORT_* fields' comment for why this is scoped to just the hole rather than the
        // whole panel.
        guiGraphics.fill(this.leftPos + VIEWPORT_LEFT, this.topPos + VIEWPORT_TOP,
                this.leftPos + VIEWPORT_LEFT + VIEWPORT_WIDTH, this.topPos + VIEWPORT_TOP + VIEWPORT_HEIGHT,
                VIEWPORT_FILL_COLOR);
        // Only the top-left 176x166 region of the 333x256 atlas holds the actual panel artwork
        // (the rest is transparent padding) - sample just that region at 1:1 scale rather than
        // stretching the whole 333x256 canvas into the 176x166 destination, which squished the
        // artwork and made it look inconsistent across GUI Scale settings.
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 333, 256);
        this.drawGridDividers(guiGraphics);
        for (int slot = 0; slot < this.menu.slotCount; slot++) {
            Slot itemSlot = this.menu.getSlot(slot);
            guiGraphics.blit(ITEM_SLOT_BACKGROUND, this.leftPos + itemSlot.x - ITEM_SLOT_BG_INSET,
                    this.topPos + itemSlot.y - ITEM_SLOT_BG_INSET, 0, 0, ITEM_SLOT_BG_SIZE, ITEM_SLOT_BG_SIZE,
                    ITEM_SLOT_BG_SIZE, ITEM_SLOT_BG_SIZE);
        }
    }

    /**
     * Draws the settings viewport's grid divider line(s) between adjacent frame slots, matching
     * the {@code gui/main_gui_*_placeholder.png} reference layouts: a horizontal line between the
     * grid's two rows (when this size has 2 distinct rows), and vertical line segment(s) between
     * the two columns within a row (skipped for a row occupied by a single slot spanning both
     * columns - see {@link wtf.vd.multiitemframe.frame.FrameSize#hasVerticalDivider(int)}).
     */
    private void drawGridDividers(GuiGraphics guiGraphics) {
        wtf.vd.multiitemframe.frame.FrameSize size = this.menu.frameSize;
        int gridLeft = this.leftPos + MultiItemFrameMenu.GRID_ORIGIN_X;
        int gridTop = this.topPos + MultiItemFrameMenu.GRID_ORIGIN_Y;
        int midX = gridLeft + MultiItemFrameMenu.CELL_WIDTH;
        int midY = gridTop + MultiItemFrameMenu.CELL_HEIGHT;
        int gridRight = gridLeft + MultiItemFrameMenu.CELL_WIDTH * 2;
        int gridBottom = gridTop + MultiItemFrameMenu.CELL_HEIGHT * 2;

        if (size.hasHorizontalDivider()) {
            guiGraphics.fill(gridLeft, midY, gridRight, midY + GRID_DIVIDER_THICKNESS, GRID_DIVIDER_COLOR);
        }
        if (size.hasVerticalDivider(0)) {
            guiGraphics.fill(midX, gridTop, midX + GRID_DIVIDER_THICKNESS, midY, GRID_DIVIDER_COLOR);
        }
        if (size.hasVerticalDivider(1)) {
            guiGraphics.fill(midX, midY, midX + GRID_DIVIDER_THICKNESS, gridBottom, GRID_DIVIDER_COLOR);
        }
    }

    /** Hides the default "Multi Item Frame" title and "Inventory" labels - the panel's own
     *  artwork ({@link #BACKGROUND}) already reads as a settings/inventory panel without them,
     *  and there's no room to lay them out cleanly across all frame sizes. */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }
}

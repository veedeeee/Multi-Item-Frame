package wtf.vd.multiitemframe.forge.frame;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import wtf.vd.multiitemframe.frame.FrameSize;
import wtf.vd.multiitemframe.forge.registry.ModMenus;

/**
 * GUI for setting the displayed items of a Multi Item Frame.
 *
 * <p>Frame slots are <b>display-only</b>: they don't hold a real item stock, they just
 * remember which item to render (see {@link #clicked}). Left-clicking one with a carried
 * item copies that item's type into the slot without touching the carried stack (nothing is
 * ever actually consumed/deposited), and middle-clicking one (in any game mode; see
 * {@code MultiItemFrameScreen#mouseClicked}) clears it back to "no item". The same applies to
 * the highlight-color button (middle-click resets to "no color"/transparent). This mirrors
 * how JEI's own ghost-ingredient drag targets work (see the {@code jei} compat package and the
 * direct-set button ids below), and lets the same mechanism serve both mouse and JEI-drag input.</p>
 */
public class MultiItemFrameMenu extends AbstractContainerMenu {

    /**
     * The settings GUI always lays frame slots out within a fixed 2x2 grid region (so every
     * {@link FrameSize} produces a same-sized, consistently-shaped panel instead of a variable
     * one) - see {@link FrameSize#columnSpan()}/{@link FrameSize#rowSpan()}. Each cell holds one
     * "row" widget group: the item slot, then the highlight-mode toggle button, then the
     * highlight-color cycle button, matching {@code gui_sample.html}'s per-slot layout.
     *
     * <p>These constants are reverse-engineered from the pixel coordinates of the
     * {@code gui/main_gui_*_placeholder.png} layout guides (measured against
     * {@code gui/main_gui_background.png}'s panel): {@code GRID_ORIGIN_*} is the transparent
     * settings viewport's top-left corner, {@code CELL_*} is the spacing between grid columns/
     * rows, and {@code GROUP_*} is the widget group's own size (item slot + 2 buttons, see
     * {@code gui/button_stack.png}) which is centered within however many grid cells a slot's
     * {@link FrameSize#columnSpan()}/{@link FrameSize#rowSpan()} spans.</p>
     */
    /** Package-private (not private): also used by {@code MultiItemFrameScreen} to draw the
     *  grid divider lines shown in the {@code gui/main_gui_*_placeholder.png} reference layouts. */
    static final int GRID_ORIGIN_X = 7;
    static final int GRID_ORIGIN_Y = 7;
    static final int CELL_WIDTH = 80;
    static final int CELL_HEIGHT = 36;
    private static final int GROUP_WIDTH = 60;
    private static final int GROUP_HEIGHT = 18;

    /*
     * Menu-button id ranges (see #clickMenuButton). Kept contiguous and non-overlapping:
     *   [0, MAX_SLOTS)                                  - cycle highlight mode for slot id
     *   [MAX_SLOTS, DIRECT_COLOR_BASE)                   - cycle highlight color for slot (id - MAX_SLOTS)
     *   [DIRECT_COLOR_BASE, DIRECT_ITEM_BASE)             - direct-set highlight color (JEI dye drag)
     *   [DIRECT_ITEM_BASE, +inf)                          - direct-set displayed item (JEI item drag)
     */
    /** First button id of the highlight-color cycle range (package-private: also used by the screen). */
    static final int COLOR_CYCLE_BASE = FrameSize.MAX_SLOTS;
    /** One slot's worth of direct-color ids: 16 dye colors plus one "clear" value. */
    static final int COLOR_ID_SPACE = DyeColor.values().length + 1;
    /** First button id of the direct-color-set range (see {@link #clickMenuButton}). */
    public static final int DIRECT_COLOR_BASE = COLOR_CYCLE_BASE + FrameSize.MAX_SLOTS;
    /** Generous per-slot id space for direct-item-set, comfortably larger than any item registry. */
    static final int ITEM_ID_SPACE = 1_000_000;
    /** First button id of the direct-item-set range (see {@link #clickMenuButton}). */
    public static final int DIRECT_ITEM_BASE = DIRECT_COLOR_BASE + FrameSize.MAX_SLOTS * COLOR_ID_SPACE;

    private final Container frameContainer;
    public final int slotCount;
    /** Also used by {@code MultiItemFrameScreen} to know which grid divider lines to draw. */
    public final FrameSize frameSize;

    public MultiItemFrameMenu(int containerId, Inventory playerInventory, MultiItemFrameEntity frame) {
        super(ModMenus.MULTI_ITEM_FRAME_MENU.get(), containerId);
        this.frameContainer = frame;
        FrameSize size = frame.getFrameSize();
        this.frameSize = size;
        this.slotCount = size.slotCount();

        for (int i = 0; i < this.slotCount; i++) {
            double[] gridPos = size.slotPosition(i);
            int cellX = GRID_ORIGIN_X + (int) Math.round(gridPos[0] * CELL_WIDTH);
            int cellY = GRID_ORIGIN_Y + (int) Math.round(gridPos[1] * CELL_HEIGHT);
            int spanWidth = size.columnSpan() * CELL_WIDTH;
            int spanHeight = size.rowSpan() * CELL_HEIGHT;
            // Center this slot's widget group (GROUP_WIDTH x GROUP_HEIGHT) within the span of
            // grid cells it was assigned (wider/taller than one cell when this FrameSize only
            // uses one column/row, per columnSpan()/rowSpan()).
            int groupX = cellX + (spanWidth - GROUP_WIDTH) / 2;
            int groupY = cellY + (spanHeight - GROUP_HEIGHT) / 2;
            this.addSlot(new Slot(frame, i, groupX, groupY));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /** Client-side reconstruction from the extra data written by {@code NetworkHooks.openScreen}. */
    public MultiItemFrameMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveFrame(playerInventory, extraData));
    }

    private static MultiItemFrameEntity resolveFrame(Inventory playerInventory, FriendlyByteBuf extraData) {
        int entityId = extraData.readVarInt();
        if (playerInventory.player.level().getEntity(entityId) instanceof MultiItemFrameEntity frame) {
            return frame;
        }
        // Entity not found (e.g. unloaded); fall back to a disposable empty frame so the GUI can still open/close cleanly.
        return new MultiItemFrameEntity(wtf.vd.multiitemframe.forge.registry.ModEntities.MULTI_ITEM_FRAME.get(),
                playerInventory.player.level());
    }

    /**
     * Frame slots are ghost/display-only (see class javadoc): a plain click copies the item
     * currently on the cursor into the slot without consuming it, and never removes the cursor's
     * item either way; middle-click always clears the slot back to "no item" regardless of game
     * mode (the screen forwards middle-clicks here itself, since vanilla only reaches this via
     * {@code ClickType.CLONE} in Creative - see {@code MultiItemFrameScreen#mouseClicked}).
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < this.slotCount) {
            if (button == 2) {
                this.frameContainer.setItem(slotId, ItemStack.EMPTY);
            } else {
                ItemStack carried = this.getCarried();
                if (!carried.isEmpty()) {
                    boolean extracted = this.frameContainer instanceof MultiItemFrameEntity frame
                            && wtf.vd.multiitemframe.forge.compat.container.ContainerContentExtractor.tryExtract(frame, slotId, carried);
                    if (!extracted) {
                        this.frameContainer.setItem(slotId, carried.copyWithCount(1));
                    }
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public void cycleHighlightMode(int slot) {
        if (slot >= 0 && slot < this.slotCount && this.frameContainer instanceof MultiItemFrameEntity frame) {
            frame.cycleHighlightMode(slot);
        }
    }

    /** Current highlight mode of a slot (used by the screen to pick the mode button's icon). */
    public wtf.vd.multiitemframe.frame.HighlightMode getHighlightMode(int slot) {
        return this.frameContainer instanceof MultiItemFrameEntity frame
                ? frame.getHighlightMode(slot)
                : wtf.vd.multiitemframe.frame.HighlightMode.FRAME;
    }

    /** Current highlight color of a slot, or {@code null} for "no highlight" (used by the screen's color button icon). */
    public net.minecraft.world.item.DyeColor getHighlightColor(int slot) {
        return this.frameContainer instanceof MultiItemFrameEntity frame ? frame.getHighlightColor(slot) : null;
    }

    /** Which kind of content a slot displays (used by the screen to pick how to render the slot icon). */
    public wtf.vd.multiitemframe.frame.DisplayContentKind getContentKind(int slot) {
        return this.frameContainer instanceof MultiItemFrameEntity frame
                ? frame.getContentKind(slot)
                : wtf.vd.multiitemframe.frame.DisplayContentKind.ITEM;
    }

    /** Registry name of the Fluid/Chemical a slot displays (used by the screen to render the slot icon). */
    public String getContentId(int slot) {
        return this.frameContainer instanceof MultiItemFrameEntity frame ? frame.getContentId(slot) : "";
    }

    private void cycleHighlightColor(int slot) {
        if (slot < 0 || slot >= this.slotCount || !(this.frameContainer instanceof MultiItemFrameEntity frame)) {
            return;
        }
        net.minecraft.world.item.DyeColor current = frame.getHighlightColor(slot);
        net.minecraft.world.item.DyeColor[] colors = net.minecraft.world.item.DyeColor.values();
        int nextIndex = current == null ? 0 : (current.getId() + 1) % (colors.length + 1);
        frame.setHighlightColor(slot, nextIndex >= colors.length ? null : colors[nextIndex]);
    }

    /** Sets a slot's highlight color directly (used by the JEI dye-drag ghost ingredient handler). */
    public void setHighlightColorDirect(int slot, net.minecraft.world.item.DyeColor color) {
        if (slot >= 0 && slot < this.slotCount && this.frameContainer instanceof MultiItemFrameEntity frame) {
            frame.setHighlightColor(slot, color);
        }
    }

    /**
     * Sets a slot's displayed item directly (used by the JEI item-drag ghost ingredient handler).
     * Routed through the same container-content extraction as a manual click (see {@link #clicked}):
     * dragging a filled container (bucket, Mekanism tank, battery, ...) from JEI's ingredient list
     * shows its content, exactly like clicking it onto the slot with the mouse would.
     */
    private void setDisplayItemDirect(int slot, int itemRegistryId) {
        if (slot < 0 || slot >= this.slotCount) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.byId(itemRegistryId);
        ItemStack stack = item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        boolean extracted = !stack.isEmpty() && this.frameContainer instanceof MultiItemFrameEntity frame
                && wtf.vd.multiitemframe.forge.compat.container.ContainerContentExtractor.tryExtract(frame, slot, stack);
        if (!extracted) {
            this.frameContainer.setItem(slot, stack);
        }
    }

    /**
     * Handles the mode/color toggle buttons rendered in the screen (vanilla's generic
     * button-click mechanism, the same one used by e.g. the Loom/Beacon screens). See the id
     * range layout documented next to the {@code *_BASE}/{@code *_SPACE} constants above.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < FrameSize.MAX_SLOTS) {
            this.cycleHighlightMode(id);
            return true;
        }
        if (id >= COLOR_CYCLE_BASE && id < DIRECT_COLOR_BASE) {
            this.cycleHighlightColor(id - COLOR_CYCLE_BASE);
            return true;
        }
        if (id >= DIRECT_COLOR_BASE && id < DIRECT_ITEM_BASE) {
            int offset = id - DIRECT_COLOR_BASE;
            int slot = offset / COLOR_ID_SPACE;
            int value = offset % COLOR_ID_SPACE;
            this.setHighlightColorDirect(slot, value >= DyeColor.values().length ? null : DyeColor.byId(value));
            return true;
        }
        if (id >= DIRECT_ITEM_BASE) {
            int offset = id - DIRECT_ITEM_BASE;
            this.setDisplayItemDirect(offset / ITEM_ID_SPACE, offset % ITEM_ID_SPACE);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < this.slotCount) {
            // Frame slots are ghost/display-only (see #clicked): there is no real item here to
            // shift-click out into the player's inventory.
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        // Shift-click only shuffles within the player's own inventory (hotbar <-> main storage);
        // it never targets frame slots, since those aren't real storage.
        int mainStart = this.slotCount;
        int mainEnd = this.slots.size() - 9;
        boolean movedFromHotbar = index >= mainEnd
                ? this.moveItemStackTo(original, mainStart, mainEnd, false)
                : this.moveItemStackTo(original, mainEnd, this.slots.size(), false);
        if (!movedFromHotbar) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moving;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.frameContainer.stillValid(player);
    }
}

package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import wtf.vd.multiitemframe.frame.FrameSize;
import wtf.vd.multiitemframe.neoforge.registry.ModMenus;

/**
 * GUI for setting the displayed items of a Multi Item Frame.
 * Frame slots are backed directly by the frame entity (synced automatically
 * via entity data); the rest of the grid is the usual player inventory so
 * items can be dragged in from there (or from JEI's ghost-slot drag for dyes
 * onto the color buttons; see the {@code jei} compat package and the
 * direct-color-set button ids below).
 */
public class MultiItemFrameMenu extends AbstractContainerMenu {

    /**
     * The settings GUI always lays frame slots out within a fixed 2x2 grid region (so every
     * {@link FrameSize} produces a same-sized, consistently-shaped panel instead of a variable
     * one) - see {@link FrameSize#columnSpan()}/{@link FrameSize#rowSpan()}. Each cell holds one
     * "row" widget group: the item slot, then the highlight-mode toggle button, then the
     * highlight-color cycle button, matching {@code gui_sample.html}'s per-slot layout.
     */
    private static final int GRID_ORIGIN_X = 34;
    private static final int GRID_ORIGIN_Y = 18;
    private static final int CELL_WIDTH = 54;
    private static final int CELL_HEIGHT = 18;
    /** First button id of the direct-color-set range (see {@link #clickMenuButton}). */
    public static final int DIRECT_COLOR_BASE = 1 + FrameSize.MAX_SLOTS * 2;

    private final Container frameContainer;
    public final int slotCount;

    public MultiItemFrameMenu(int containerId, Inventory playerInventory, MultiItemFrameEntity frame) {
        super(ModMenus.MULTI_ITEM_FRAME_MENU.get(), containerId);
        this.frameContainer = frame;
        FrameSize size = frame.getFrameSize();
        this.slotCount = size.slotCount();

        for (int i = 0; i < this.slotCount; i++) {
            int[] gridPos = size.slotPosition(i);
            int cellX = GRID_ORIGIN_X + gridPos[0] * CELL_WIDTH;
            int cellY = GRID_ORIGIN_Y + gridPos[1] * CELL_HEIGHT;
            int cellWidth = size.columnSpan() * CELL_WIDTH;
            int cellHeight = size.rowSpan() * CELL_HEIGHT;
            // Center this slot's widget group (item slot + buttons, CELL_WIDTH x CELL_HEIGHT)
            // within the cell area it was assigned (wider/taller than one cell when this
            // FrameSize only uses one column/row, per columnSpan()/rowSpan()).
            int groupX = cellX + (cellWidth - CELL_WIDTH) / 2;
            int groupY = cellY + (cellHeight - CELL_HEIGHT) / 2;
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

    /** Fallback constructor used when the client only has a detached container (e.g. entity not yet loaded). */
    public MultiItemFrameMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveFrame(playerInventory, extraData));
    }

    private static MultiItemFrameEntity resolveFrame(Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        int entityId = extraData.readVarInt();
        if (playerInventory.player.level().getEntity(entityId) instanceof MultiItemFrameEntity frame) {
            return frame;
        }
        // Entity not found (e.g. unloaded); fall back to a disposable empty frame so the GUI can still open/close cleanly.
        return new MultiItemFrameEntity(wtf.vd.multiitemframe.neoforge.registry.ModEntities.MULTI_ITEM_FRAME.get(),
                playerInventory.player.level());
    }

    /** Middle-click (creative "clone") on a frame slot erases the displayed item instead of duplicating it. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.CLONE && slotId >= 0 && slotId < this.slotCount) {
            this.frameContainer.setItem(slotId, ItemStack.EMPTY);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public void toggleBackground() {
        if (this.frameContainer instanceof MultiItemFrameEntity frame) {
            frame.toggleBackground();
        }
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
     * Handles the mode/color toggle buttons rendered in the screen (vanilla's generic
     * button-click mechanism, the same one used by e.g. the Loom/Beacon screens).
     * Button id layout: {@code 0} = background toggle, {@code 1..4} = cycle highlight
     * mode for slot {@code id-1}, {@code 5..8} = cycle highlight color for slot
     * {@code id-5}, {@code 9..} = direct-set highlight color to
     * {@code DyeColor.byId((id - DIRECT_COLOR_BASE) % 16)} for slot
     * {@code (id - DIRECT_COLOR_BASE) / 16} (used by JEI's dye-drag ghost target).
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            this.toggleBackground();
            return true;
        }
        if (id >= 1 && id <= FrameSize.MAX_SLOTS) {
            this.cycleHighlightMode(id - 1);
            return true;
        }
        if (id >= 1 + FrameSize.MAX_SLOTS && id <= 1 + FrameSize.MAX_SLOTS * 2) {
            this.cycleHighlightColor(id - 1 - FrameSize.MAX_SLOTS);
            return true;
        }
        if (id >= DIRECT_COLOR_BASE && id < DIRECT_COLOR_BASE + FrameSize.MAX_SLOTS * 16) {
            int offset = id - DIRECT_COLOR_BASE;
            this.setHighlightColorDirect(offset / 16, net.minecraft.world.item.DyeColor.byId(offset % 16));
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (index < this.slotCount) {
            // Moving out of a frame slot into the player inventory.
            if (!this.moveItemStackTo(original, this.slotCount, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Moving from the player inventory into the first free frame slot.
            if (!this.moveItemStackTo(original, 0, this.slotCount, false)) {
                return ItemStack.EMPTY;
            }
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

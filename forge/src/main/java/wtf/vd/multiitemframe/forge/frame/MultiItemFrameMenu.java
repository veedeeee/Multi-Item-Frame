package wtf.vd.multiitemframe.forge.frame;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import wtf.vd.multiitemframe.frame.FrameSize;
import wtf.vd.multiitemframe.forge.registry.ModMenus;

/**
 * GUI for setting the displayed items of a Multi Item Frame.
 * Frame slots are backed directly by the frame entity (synced automatically
 * via entity data); the rest of the grid is the usual player inventory so
 * items can be dragged in from there (or from JEI's ghost-slot drag, once
 * that integration is added).
 */
public class MultiItemFrameMenu extends AbstractContainerMenu {

    private static final int FRAME_SLOT_X = 62;
    private static final int FRAME_SLOT_Y = 17;
    private static final int SLOT_SIZE = 18;

    private final Container frameContainer;
    public final int slotCount;

    public MultiItemFrameMenu(int containerId, Inventory playerInventory, MultiItemFrameEntity frame) {
        super(ModMenus.MULTI_ITEM_FRAME_MENU.get(), containerId);
        this.frameContainer = frame;
        FrameSize size = frame.getFrameSize();
        this.slotCount = size.slotCount();

        for (int i = 0; i < this.slotCount; i++) {
            int[] gridPos = size.slotPosition(i);
            this.addSlot(new Slot(frame, i,
                    FRAME_SLOT_X + gridPos[0] * SLOT_SIZE,
                    FRAME_SLOT_Y + gridPos[1] * SLOT_SIZE));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * SLOT_SIZE, 84 + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * SLOT_SIZE, 142));
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

    private void cycleHighlightColor(int slot) {
        if (slot < 0 || slot >= this.slotCount || !(this.frameContainer instanceof MultiItemFrameEntity frame)) {
            return;
        }
        net.minecraft.world.item.DyeColor current = frame.getHighlightColor(slot);
        net.minecraft.world.item.DyeColor[] colors = net.minecraft.world.item.DyeColor.values();
        int nextIndex = current == null ? 0 : (current.getId() + 1) % (colors.length + 1);
        frame.setHighlightColor(slot, nextIndex >= colors.length ? null : colors[nextIndex]);
    }

    /**
     * Handles the mode/color toggle buttons rendered in the screen (vanilla's generic
     * button-click mechanism, the same one used by e.g. the Loom/Beacon screens).
     * Button id layout: {@code 0} = background toggle, {@code 1..4} = cycle highlight
     * mode for slot {@code id-1}, {@code 5..8} = cycle highlight color for slot {@code id-5}.
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

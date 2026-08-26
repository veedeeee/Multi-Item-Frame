package wtf.vd.multiitemframe.forge.compat.ae2;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameEntity;

/**
 * AE2 Memory Card support (Ch.5).
 *
 * <p>AE2's own generic settings-export mechanism ({@code IConfigurableObject}/
 * {@code IPriorityHost}/{@code IConfigInvHost}) only round-trips a fixed set of
 * AE2-specific concepts (upgrades, priority, a config inventory, enum-based
 * config settings) and cannot carry our frame's custom highlight mode/color
 * data. Its shape also differs between AE2 15.x (Forge/1.20.1: a generic
 * {@code setMemoryCardContents(stack, name, CompoundTag)}) and AE2 19.x
 * (NeoForge/1.21.1: DataComponent based, no longer freeform NBT). To keep both
 * loaders byte-for-byte identical we bypass all of that and store our own
 * settings blob directly under our own mod-namespaced NBT key on the card's
 * ItemStack, only using {@link IMemoryCard} as a type marker for "is this an
 * AE2 memory card" and for the vanilla settings saved/loaded chat
 * notifications.</p>
 */
public final class Ae2MemoryCardCompat {

    private static final String TAG_KEY = "multiitemframe:frame_settings";

    private Ae2MemoryCardCompat() {
    }

    public static boolean isMemoryCard(ItemStack stack) {
        return stack.getItem() instanceof IMemoryCard;
    }

    /** Shift-click saves the frame's settings to the card; a plain click restores them. */
    public static void handle(MultiItemFrameEntity frame, Player player, ItemStack card) {
        IMemoryCard memoryCard = (IMemoryCard) card.getItem();
        if (player.isShiftKeyDown()) {
            writeTag(card, frame.copySettings());
            memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
        } else {
            CompoundTag data = readTag(card);
            if (data == null) {
                memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                return;
            }
            frame.pasteSettings(data);
            memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
        }
    }

    private static void writeTag(ItemStack card, CompoundTag data) {
        card.getOrCreateTag().put(TAG_KEY, data);
    }

    private static CompoundTag readTag(ItemStack card) {
        CompoundTag tag = card.getTag();
        return tag != null && tag.contains(TAG_KEY) ? tag.getCompound(TAG_KEY) : null;
    }
}

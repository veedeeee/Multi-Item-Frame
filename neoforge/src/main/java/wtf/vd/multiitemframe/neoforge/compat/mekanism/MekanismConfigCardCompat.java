package wtf.vd.multiitemframe.neoforge.compat.mekanism;

import mekanism.api.SerializationConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameEntity;

/**
 * Mekanism Configuration Card support (Ch.5).
 *
 * <p>Mekanism's own {@code IConfigCardAccess} capability is only looked up on
 * {@code BlockEntity} targets (see {@code ItemConfigurationCard.useOn}), so it
 * never fires for an entity-based frame. Instead we replicate the card's own
 * NBT layout ({@code mek_data -> data / data_name}, keys from
 * {@code mekanism.api.SerializationConstants}) directly inside the card's
 * {@code minecraft:custom_data} component so the vanilla item's tooltip
 * ("Has Data: ...") keeps working, matching by our own {@code dataName}
 * marker instead of a {@code BlockEntityType}.</p>
 */
public final class MekanismConfigCardCompat {

    private static final String CONFIG_CARD_NAME_KEY = "gui.multiitemframe.config_card_name";
    private static final String ITEM_ID = "mekanism:configuration_card";

    private MekanismConfigCardCompat() {
    }

    public static boolean isConfigCard(ItemStack stack) {
        return ITEM_ID.equals(String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    /** Shift-click saves the frame's settings to the card; a plain click restores them. */
    public static void handle(MultiItemFrameEntity frame, Player player, ItemStack card) {
        if (player.isShiftKeyDown()) {
            CompoundTag data = frame.copySettings();
            CompoundTag mekData = new CompoundTag();
            mekData.putString(SerializationConstants.DATA_NAME, CONFIG_CARD_NAME_KEY);
            mekData.put(SerializationConstants.DATA, data);
            writeMekData(card, mekData);
            player.displayClientMessage(Component.translatable("gui.multiitemframe.config_card_saved"), true);
        } else {
            CompoundTag mekData = readMekData(card);
            if (mekData == null || !CONFIG_CARD_NAME_KEY.equals(mekData.getString(SerializationConstants.DATA_NAME))) {
                player.displayClientMessage(Component.translatable("gui.multiitemframe.config_card_invalid"), true);
                return;
            }
            frame.pasteSettings(mekData.getCompound(SerializationConstants.DATA));
            player.displayClientMessage(Component.translatable("gui.multiitemframe.config_card_loaded"), true);
        }
    }

    private static void writeMekData(ItemStack card, CompoundTag mekData) {
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(SerializationConstants.MEK_DATA, mekData);
        card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag readMekData(ItemStack card) {
        CustomData customData = card.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.copyTag();
        return tag.contains(SerializationConstants.MEK_DATA) ? tag.getCompound(SerializationConstants.MEK_DATA) : null;
    }
}

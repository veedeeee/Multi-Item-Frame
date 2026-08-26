package wtf.vd.multiitemframe.forge.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameScreen;

/**
 * JEI integration (Ch.5).
 *
 * <p>Frame item slots are ghost/display-only (they hold no real items), so JEI's
 * built-in slot drag-and-drop (which inserts/extracts real stacks) does not apply
 * to them. This plugin instead registers an {@link IGhostIngredientHandler} that
 * targets each frame's item slot for any ingredient (setting the displayed item
 * via {@link MultiItemFrameScreen#sendDirectItem}), and additionally targets each
 * frame's color button when the dragged ingredient is a dye (setting the highlight
 * color via {@link MultiItemFrameScreen#sendDirectColor}).</p>
 */
@JeiPlugin
public final class MultiItemFrameJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            new ResourceLocation(MultiItemFrame.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(MultiItemFrameScreen.class, new FrameGhostIngredientHandler());
    }

    /**
     * Lets any item be dragged from JEI's ingredient list onto a frame's item slot (sets the
     * displayed item), and lets dyes additionally be dropped onto a frame's color button (sets
     * the highlight color).
     */
    private static final class FrameGhostIngredientHandler implements IGhostIngredientHandler<MultiItemFrameScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(MultiItemFrameScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            Optional<ItemStack> stackOptional = ingredient.getItemStack();
            if (stackOptional.isEmpty()) {
                return List.of();
            }
            ItemStack stack = stackOptional.get();
            List<Target<I>> targets = new ArrayList<>();

            for (int slot = 0; slot < gui.getSlotCount(); slot++) {
                int slotIndex = slot;
                Rect2i itemArea = gui.getItemSlotArea(slot);
                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return itemArea;
                    }

                    @Override
                    public void accept(I ingredient) {
                        gui.sendDirectItem(slotIndex, stack);
                    }
                });
            }

            if (stack.getItem() instanceof DyeItem dyeItem) {
                DyeColor color = dyeItem.getDyeColor();
                for (int slot = 0; slot < gui.getSlotCount(); slot++) {
                    int slotIndex = slot;
                    Rect2i colorArea = gui.getColorButtonArea(slot);
                    targets.add(new Target<>() {
                        @Override
                        public Rect2i getArea() {
                            return colorArea;
                        }

                        @Override
                        public void accept(I ingredient) {
                            gui.sendDirectColor(slotIndex, color);
                        }
                    });
                }
            }

            return targets;
        }

        @Override
        public void onComplete() {
            // No cleanup needed: sendDirectItem/sendDirectColor already sent the button click on accept().
        }
    }
}

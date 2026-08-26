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
 * <p>Real item slots in {@link wtf.vd.multiitemframe.forge.frame.MultiItemFrameMenu}
 * already accept JEI's built-in "drag ingredient onto a slot" behavior with no
 * extra code (JEI supports that for any vanilla {@code Slot}). What JEI can't
 * do on its own is drop a dye onto our non-slot color-toggle buttons, so this
 * plugin adds a {@link IGhostIngredientHandler} for that one case.</p>
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
        registration.addGhostIngredientHandler(MultiItemFrameScreen.class, new DyeGhostIngredientHandler());
    }

    /** Lets a dye item be dragged from JEI's ingredient list directly onto a frame slot's color button. */
    private static final class DyeGhostIngredientHandler implements IGhostIngredientHandler<MultiItemFrameScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(MultiItemFrameScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            Optional<ItemStack> stack = ingredient.getItemStack();
            if (stack.isEmpty() || !(stack.get().getItem() instanceof DyeItem dyeItem)) {
                return List.of();
            }
            DyeColor color = dyeItem.getDyeColor();
            List<Target<I>> targets = new ArrayList<>();
            for (int slot = 0; slot < gui.getSlotCount(); slot++) {
                int slotIndex = slot;
                Rect2i area = gui.getColorButtonArea(slot);
                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return area;
                    }

                    @Override
                    public void accept(I ingredient) {
                        gui.sendDirectColor(slotIndex, color);
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
            // No cleanup needed: sendDirectColor already sent the button click on accept().
        }
    }
}

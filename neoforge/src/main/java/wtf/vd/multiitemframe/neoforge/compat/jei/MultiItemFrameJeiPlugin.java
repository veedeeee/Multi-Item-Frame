package wtf.vd.multiitemframe.neoforge.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mekanism.api.chemical.ChemicalStack;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.frame.DisplayContentKind;
import wtf.vd.multiitemframe.neoforge.compat.mekanism.MekanismChemicalCompat;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameScreen;

/**
 * JEI integration (Ch.5).
 *
 * <p>Real item slots in {@link wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameMenu}
 * already accept JEI's built-in "drag ingredient onto a slot" behavior with no
 * extra code (JEI supports that for any vanilla {@code Slot}). What JEI can't
 * do on its own is drop a dye onto our non-slot color-toggle buttons, or drop a raw
 * (non-item) Fluid/Mekanism-chemical ingredient onto a frame slot - dragging an item
 * (e.g. a bucket) while simultaneously right-clicking to pick a different fluid/chemical
 * is awkward-to-impossible as a mouse gesture, so those need their own ghost handler.</p>
 */
@JeiPlugin
public final class MultiItemFrameJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(MultiItemFrame.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(MultiItemFrameScreen.class, new FrameGhostIngredientHandler());
    }

    /**
     * Lets a dye item be dragged from JEI's ingredient list directly onto a frame slot's color
     * button, and lets raw Fluid/Mekanism-chemical ingredients be dropped onto a frame's item
     * slot directly (sets the displayed content, same as dragging a filled container item would).
     */
    private static final class FrameGhostIngredientHandler implements IGhostIngredientHandler<MultiItemFrameScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(MultiItemFrameScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            Optional<ItemStack> stack = ingredient.getItemStack();
            if (stack.isPresent()) {
                return getDyeTargets(gui, stack.get());
            }
            return getRawContentTargets(gui, ingredient.getIngredient());
        }

        private <I> List<Target<I>> getDyeTargets(MultiItemFrameScreen gui, ItemStack stack) {
            if (!(stack.getItem() instanceof DyeItem dyeItem)) {
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

        /**
         * Raw (non-item) ingredient support: a {@code FluidStack} (any mod's fluid, dragged
         * straight from JEI's ingredient list) or, if Mekanism is loaded, a {@code ChemicalStack}.
         * Each resolves to a {@link DisplayContentKind} plus a stable registry int id (see
         * {@code MultiItemFrameMenu#DIRECT_CONTENT_BASE}), since the underlying button-click
         * mechanism can only transmit a plain {@code int}.
         */
        private <I> List<Target<I>> getRawContentTargets(MultiItemFrameScreen gui, Object rawIngredient) {
            DisplayContentKind kind;
            int registryId;
            if (rawIngredient instanceof FluidStack fluidStack && !fluidStack.isEmpty()) {
                Fluid fluid = fluidStack.getFluid();
                if (fluid == null || fluid == Fluids.EMPTY) {
                    return List.of();
                }
                kind = DisplayContentKind.FLUID;
                registryId = BuiltInRegistries.FLUID.getId(fluid);
            } else if (ModList.get().isLoaded("mekanism") && rawIngredient instanceof ChemicalStack chemicalStack
                    && !chemicalStack.isEmpty()) {
                kind = DisplayContentKind.CHEMICAL;
                String contentId = String.valueOf(
                        mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.getKey(chemicalStack.getChemical()));
                registryId = MekanismChemicalCompat.getRegistryId(contentId);
            } else {
                return List.of();
            }
            if (registryId < 0) {
                return List.of();
            }

            List<Target<I>> targets = new ArrayList<>();
            for (int slot = 0; slot < gui.getSlotCount(); slot++) {
                int slotIndex = slot;
                DisplayContentKind targetKind = kind;
                int targetRegistryId = registryId;
                Rect2i itemArea = gui.getItemSlotArea(slot);
                targets.add(new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return itemArea;
                    }

                    @Override
                    public void accept(I ingredient) {
                        gui.sendDirectContent(slotIndex, targetKind, targetRegistryId);
                    }
                });
            }
            return targets;
        }

        @Override
        public void onComplete() {
            // No cleanup needed: sendDirectColor/sendDirectContent already sent the button click on accept().
        }
    }
}


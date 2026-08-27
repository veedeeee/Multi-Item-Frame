package wtf.vd.multiitemframe.forge.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.forge.compat.mekanism.MekanismChemicalCompat;
import wtf.vd.multiitemframe.frame.DisplayContentKind;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameScreen;

/**
 * JEI integration (Ch.5).
 *
 * <p>Frame item slots are ghost/display-only (they hold no real items), so JEI's
 * built-in slot drag-and-drop (which inserts/extracts real stacks) does not apply
 * to them. This plugin instead registers an {@link IGhostIngredientHandler} that
 * targets each frame's item slot for any ingredient (setting the displayed item
 * via {@link MultiItemFrameScreen#sendDirectItem}), additionally targets each
 * frame's color button when the dragged ingredient is a dye (setting the highlight
 * color via {@link MultiItemFrameScreen#sendDirectColor}), and also targets each
 * frame's item slot for raw Fluid/Mekanism-chemical ingredients dragged straight from
 * JEI's ingredient list - not attached to any container item - via
 * {@link MultiItemFrameScreen#sendDirectContent}. That last case matters because dragging
 * an item (e.g. a bucket) from JEI while simultaneously right-clicking to pick a different
 * fluid/chemical is awkward-to-impossible as a mouse gesture, so raw ingredients need their
 * own direct drop target.</p>
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
     * displayed item), lets dyes additionally be dropped onto a frame's color button (sets the
     * highlight color), and lets raw Fluid/Mekanism-chemical ingredients be dropped onto a
     * frame's item slot directly (sets the displayed content, same as dragging a filled
     * container item would).
     */
    private static final class FrameGhostIngredientHandler implements IGhostIngredientHandler<MultiItemFrameScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(MultiItemFrameScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            Optional<ItemStack> stackOptional = ingredient.getItemStack();
            if (stackOptional.isPresent()) {
                return getItemTargets(gui, stackOptional.get());
            }
            return getRawContentTargets(gui, ingredient.getIngredient());
        }

        private <I> List<Target<I>> getItemTargets(MultiItemFrameScreen gui, ItemStack stack) {
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

        /**
         * Raw (non-item) ingredient support: a {@code FluidStack} (any mod's fluid, dragged
         * straight from JEI's ingredient list) or, if Mekanism is loaded, a Gas/Infusion/Pigment/
         * SlurryStack. Each resolves to a {@link DisplayContentKind} plus a Forge per-session
         * integer registry id (see {@code MultiItemFrameMenu#DIRECT_CONTENT_BASE}), since the
         * underlying button-click mechanism can only transmit a plain {@code int}.
         */
        private <I> List<Target<I>> getRawContentTargets(MultiItemFrameScreen gui, Object rawIngredient) {
            DisplayContentKind kind;
            int registryId;
            if (rawIngredient instanceof FluidStack fluidStack && !fluidStack.isEmpty()) {
                Fluid fluid = fluidStack.getFluid();
                if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
                    return List.of();
                }
                kind = DisplayContentKind.FLUID;
                registryId = BuiltInRegistries.FLUID.getId(fluid);
            } else if (ModList.get().isLoaded("mekanism") && rawIngredient instanceof ChemicalStack<?> chemicalStack
                    && !chemicalStack.isEmpty()) {
                kind = chemicalKindOf(chemicalStack);
                String contentId = String.valueOf(chemicalStack.getType().getRegistryName());
                registryId = kind == null ? -1 : MekanismChemicalCompat.getRegistryId(kind, contentId);
            } else {
                return List.of();
            }
            if (kind == null || registryId < 0) {
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

        private static DisplayContentKind chemicalKindOf(ChemicalStack<?> chemicalStack) {
            if (chemicalStack instanceof GasStack) {
                return DisplayContentKind.GAS;
            }
            if (chemicalStack instanceof InfusionStack) {
                return DisplayContentKind.INFUSION;
            }
            if (chemicalStack instanceof PigmentStack) {
                return DisplayContentKind.PIGMENT;
            }
            if (chemicalStack instanceof SlurryStack) {
                return DisplayContentKind.SLURRY;
            }
            return null;
        }

        @Override
        public void onComplete() {
            // No cleanup needed: sendDirectItem/sendDirectColor/sendDirectContent already sent the button click on accept().
        }
    }
}


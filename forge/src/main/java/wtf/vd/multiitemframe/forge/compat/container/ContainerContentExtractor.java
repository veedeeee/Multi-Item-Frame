package wtf.vd.multiitemframe.forge.compat.container;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import wtf.vd.multiitemframe.forge.compat.mekanism.MekanismChemicalCompat;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameEntity;
import wtf.vd.multiitemframe.frame.DisplayContentKind;

/**
 * Extracts a "content type to display" from a container item clicked onto a frame's GUI item
 * slot (see {@code MultiItemFrameMenu#clicked}), instead of displaying the container item itself.
 * Only the type is shown - no quantity/amount - matching the existing item-ghost convention
 * (fixed count-1, purely cosmetic). Checked in order: Fluid (vanilla Forge capability, covers
 * buckets/tanks from any mod), Mekanism Gas/Infusion/Pigment/Slurry (only if Mekanism is loaded),
 * then generic FE energy (vanilla Forge capability, covers batteries/cubes from any mod). If the
 * container is empty (or the stack exposes none of these capabilities), this returns {@code
 * false} and the caller falls back to displaying the item itself.
 */
public final class ContainerContentExtractor {

    private ContainerContentExtractor() {
    }

    public static boolean tryExtract(MultiItemFrameEntity frame, int slot, ItemStack carried) {
        if (tryExtractFluid(frame, slot, carried)) {
            return true;
        }
        if (ModList.get().isLoaded("mekanism") && MekanismChemicalCompat.tryExtract(frame, slot, carried)) {
            return true;
        }
        return tryExtractEnergy(frame, slot, carried);
    }

    private static boolean tryExtractFluid(MultiItemFrameEntity frame, int slot, ItemStack carried) {
        IFluidHandlerItem handler = carried.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluidStack = handler.getFluidInTank(tank);
            if (!fluidStack.isEmpty()) {
                String id = String.valueOf(ForgeRegistries.FLUIDS.getKey(fluidStack.getFluid()));
                frame.setDisplayContent(slot, DisplayContentKind.FLUID, id);
                return true;
            }
        }
        return false;
    }

    private static boolean tryExtractEnergy(MultiItemFrameEntity frame, int slot, ItemStack carried) {
        IEnergyStorage energy = carried.getCapability(ForgeCapabilities.ENERGY).resolve().orElse(null);
        if (energy != null && energy.getEnergyStored() > 0) {
            frame.setDisplayContent(slot, DisplayContentKind.ENERGY, "");
            return true;
        }
        return false;
    }
}

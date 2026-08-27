package wtf.vd.multiitemframe.neoforge.compat.container;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import wtf.vd.multiitemframe.frame.DisplayContentKind;
import wtf.vd.multiitemframe.neoforge.compat.mekanism.MekanismChemicalCompat;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameEntity;

/**
 * Extracts a "content type to display" from a container item clicked onto a frame's GUI item
 * slot (see {@code MultiItemFrameMenu#clicked}), instead of displaying the container item itself.
 * Only the type is shown - no quantity/amount - matching the existing item-ghost convention
 * (fixed count-1, purely cosmetic). Checked in order: Fluid (NeoForge's built-in capability,
 * covers buckets/tanks from any mod), Mekanism chemical (only if Mekanism is loaded), then
 * generic FE energy (NeoForge's built-in capability, covers batteries/cubes from any mod). If
 * the container is empty (or the stack exposes none of these capabilities), this returns
 * {@code false} and the caller falls back to displaying the item itself.
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
        IFluidHandlerItem handler = carried.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluidStack = handler.getFluidInTank(tank);
            if (!fluidStack.isEmpty()) {
                String id = String.valueOf(
                        net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluidStack.getFluid()));
                frame.setDisplayContent(slot, DisplayContentKind.FLUID, id);
                return true;
            }
        }
        return false;
    }

    private static boolean tryExtractEnergy(MultiItemFrameEntity frame, int slot, ItemStack carried) {
        IEnergyStorage energy = carried.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy != null && energy.getEnergyStored() > 0) {
            frame.setDisplayContent(slot, DisplayContentKind.ENERGY, "");
            return true;
        }
        return false;
    }
}

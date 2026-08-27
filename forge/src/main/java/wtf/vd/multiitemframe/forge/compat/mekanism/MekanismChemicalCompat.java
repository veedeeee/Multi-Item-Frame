package wtf.vd.multiitemframe.forge.compat.mekanism;

import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.MekanismAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameEntity;
import wtf.vd.multiitemframe.frame.DisplayContentKind;

/**
 * Mekanism Gas/Infusion/Pigment/Slurry support for the container-content display feature (see
 * {@code compat.container.ContainerContentExtractor}). All 4 "chemical" kinds share an identical
 * tank-based API shape ({@link IChemicalHandler}), so extraction/lookup is written generically
 * against that shared interface and only specialized per-kind at the edges (capability token,
 * registry, {@link DisplayContentKind}).
 *
 * <p>Capability tokens are obtained via {@link CapabilityManager#get} ourselves (the same way
 * {@code mekanism.common.capabilities.Capabilities} does internally) rather than depending on
 * that class directly, since only Mekanism's {@code api} artifact - which does not include
 * {@code mekanism.common.*} - is a compile-time dependency here (see {@code build.gradle}'s
 * Ch.5 notes); Forge's capability registry dedupes by the reified generic type, so this resolves
 * to the exact same {@code Capability<IGasHandler>} instance Mekanism itself registers.</p>
 */
public final class MekanismChemicalCompat {

    private static final Capability<IGasHandler> GAS_HANDLER = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<IInfusionHandler> INFUSION_HANDLER = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<IPigmentHandler> PIGMENT_HANDLER = CapabilityManager.get(new CapabilityToken<>() {});
    private static final Capability<ISlurryHandler> SLURRY_HANDLER = CapabilityManager.get(new CapabilityToken<>() {});

    private MekanismChemicalCompat() {
    }

    public static boolean tryExtract(MultiItemFrameEntity frame, int slot, ItemStack carried) {
        if (tryExtract(frame, slot, carried, GAS_HANDLER, DisplayContentKind.GAS)) {
            return true;
        }
        if (tryExtract(frame, slot, carried, INFUSION_HANDLER, DisplayContentKind.INFUSION)) {
            return true;
        }
        if (tryExtract(frame, slot, carried, PIGMENT_HANDLER, DisplayContentKind.PIGMENT)) {
            return true;
        }
        return tryExtract(frame, slot, carried, SLURRY_HANDLER, DisplayContentKind.SLURRY);
    }

    private static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> boolean tryExtract(
            MultiItemFrameEntity frame, int slot, ItemStack carried, Capability<? extends IChemicalHandler<CHEMICAL, STACK>> capability,
            DisplayContentKind kind) {
        IChemicalHandler<CHEMICAL, STACK> handler = carried.getCapability(capability).resolve().orElse(null);
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            STACK stack = handler.getChemicalInTank(tank);
            if (!stack.isEmpty()) {
                frame.setDisplayContent(slot, kind, String.valueOf(stack.getType().getRegistryName()));
                return true;
            }
        }
        return false;
    }

    /** Sprite location (in the vanilla block atlas, matching {@code MekanismRenderer#getSprite})
     *  of the icon for a Gas/Infusion/Pigment/Slurry content id, or {@code null} if the id is no
     *  longer registered (e.g. the mod that added it was removed). */
    public static ResourceLocation getIcon(DisplayContentKind kind, String id) {
        Chemical<?> chemical = resolveChemical(kind, id);
        return chemical == null ? null : chemical.getIcon();
    }

    /** RGB tint (no alpha) for a Gas/Infusion/Pigment/Slurry content id, white (no tint) if unresolved. */
    public static int getTint(DisplayContentKind kind, String id) {
        Chemical<?> chemical = resolveChemical(kind, id);
        return chemical == null ? 0xFFFFFF : chemical.getTint();
    }

    private static Chemical<?> resolveChemical(DisplayContentKind kind, String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return null;
        }
        return switch (kind) {
            case GAS -> MekanismAPI.gasRegistry().getValue(rl);
            case INFUSION -> MekanismAPI.infuseTypeRegistry().getValue(rl);
            case PIGMENT -> MekanismAPI.pigmentRegistry().getValue(rl);
            case SLURRY -> MekanismAPI.slurryRegistry().getValue(rl);
            default -> null;
        };
    }
}

package wtf.vd.multiitemframe.neoforge.compat.mekanism;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import wtf.vd.multiitemframe.frame.DisplayContentKind;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameEntity;

/**
 * Mekanism chemical support for the container-content display feature (see
 * {@code compat.container.ContainerContentExtractor}). Unlike the Forge (1.20.1) module - whose
 * Mekanism version still splits "chemicals" into 4 distinct Gas/Infusion/Pigment/Slurry types -
 * the NeoForge (1.21.1) module's Mekanism version unified all 4 into a single generic
 * {@link Chemical}/{@link ChemicalStack}/{@link IChemicalHandler} API (see
 * {@code mekanism.api.chemical}), so there is only one {@link DisplayContentKind#CHEMICAL} kind
 * to handle here, not 4.
 *
 * <p>The capability token itself is declared independently via {@link ItemCapability#createVoid}
 * rather than depending on Mekanism's own {@code mekanism.common.capabilities.Capabilities.CHEMICAL}
 * (which lives in {@code mekanism.common}, not included in the {@code api}-only compile-time
 * dependency this project uses - see {@code build.gradle}'s Ch.5 notes). NeoForge's capability
 * registry dedupes {@code ItemCapability}s by (registration name, queried type, context type), so
 * registering the same name ({@code "mekanism:chemical_handler"}) and type ({@link
 * IChemicalHandler}) here resolves to the exact same capability instance Mekanism itself
 * registers internally.</p>
 */
public final class MekanismChemicalCompat {

    private static final ItemCapability<IChemicalHandler, Void> CHEMICAL_HANDLER = ItemCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);

    private MekanismChemicalCompat() {
    }

    public static boolean tryExtract(MultiItemFrameEntity frame, int slot, ItemStack carried) {
        IChemicalHandler handler = carried.getCapability(CHEMICAL_HANDLER);
        if (handler == null) {
            return false;
        }
        for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
            ChemicalStack stack = handler.getChemicalInTank(tank);
            if (!stack.isEmpty()) {
                ResourceLocation id = MekanismAPI.CHEMICAL_REGISTRY.getKey(stack.getChemical());
                if (id != null) {
                    frame.setDisplayContent(slot, DisplayContentKind.CHEMICAL, id.toString());
                    return true;
                }
            }
        }
        return false;
    }

    /** Sprite location (in the vanilla block atlas) of the icon for a chemical content id, or
     *  {@code null} if the id is no longer registered (e.g. the mod that added it was removed). */
    public static ResourceLocation getIcon(String id) {
        Chemical chemical = resolveChemical(id);
        return chemical == null ? null : chemical.getIcon();
    }

    /** RGB tint (no alpha) for a chemical content id, white (no tint) if unresolved. */
    public static int getTint(String id) {
        Chemical chemical = resolveChemical(id);
        return chemical == null ? 0xFFFFFF : chemical.getTint();
    }

    private static Chemical resolveChemical(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : MekanismAPI.CHEMICAL_REGISTRY.get(rl);
    }
}

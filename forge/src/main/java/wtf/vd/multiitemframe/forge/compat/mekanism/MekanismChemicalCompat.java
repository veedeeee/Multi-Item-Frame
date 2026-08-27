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
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
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

    /** Display name of a Gas/Infusion/Pigment/Slurry content id (used by the Jade tooltip
     *  provider - see {@code compat.jade}), or {@code null} if the id is no longer registered. */
    public static net.minecraft.network.chat.Component getName(DisplayContentKind kind, String id) {
        Chemical<?> chemical = resolveChemical(kind, id);
        return chemical == null ? null : chemical.getTextComponent();
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

    /**
     * Forge's own stable per-session integer registry id for a Gas/Infusion/Pigment/Slurry
     * content id, or {@code -1} if unresolved. Used to encode a JEI-dragged chemical ingredient
     * into a menu button-click id (see {@code MultiItemFrameMenu#DIRECT_CONTENT_BASE}), the same
     * trick already used for plain items via {@code BuiltInRegistries.ITEM.getId}.
     *
     * <p>{@link IForgeRegistry} itself doesn't expose an int-id lookup, but Mekanism's chemical
     * registries (obtained via {@link MekanismAPI}) are always backed by Forge's own {@link
     * ForgeRegistry} implementation, which does - so this downcasts to reach it, mirroring how
     * Forge's registry sync packet works internally.</p>
     */
    public static int getRegistryId(DisplayContentKind kind, String id) {
        Chemical<?> chemical = resolveChemical(kind, id);
        if (chemical == null) {
            return -1;
        }
        return switch (kind) {
            case GAS -> rawId(MekanismAPI.gasRegistry(), (mekanism.api.chemical.gas.Gas) chemical);
            case INFUSION -> rawId(MekanismAPI.infuseTypeRegistry(), (mekanism.api.chemical.infuse.InfuseType) chemical);
            case PIGMENT -> rawId(MekanismAPI.pigmentRegistry(), (mekanism.api.chemical.pigment.Pigment) chemical);
            case SLURRY -> rawId(MekanismAPI.slurryRegistry(), (mekanism.api.chemical.slurry.Slurry) chemical);
            default -> -1;
        };
    }

    /** Reverse of {@link #getRegistryId}: resolves a chemical back to its content id string
     *  (registry name) from Forge's per-session integer registry id, or {@code null} if the id
     *  no longer resolves to anything (e.g. out of range, or the value was removed). */
    public static String resolveContentId(DisplayContentKind kind, int registryId) {
        return switch (kind) {
            case GAS -> rawKey(MekanismAPI.gasRegistry(), registryId);
            case INFUSION -> rawKey(MekanismAPI.infuseTypeRegistry(), registryId);
            case PIGMENT -> rawKey(MekanismAPI.pigmentRegistry(), registryId);
            case SLURRY -> rawKey(MekanismAPI.slurryRegistry(), registryId);
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> int rawId(IForgeRegistry<T> registry, T value) {
        return registry instanceof ForgeRegistry<T> forgeRegistry ? forgeRegistry.getID(value) : -1;
    }

    @SuppressWarnings("unchecked")
    private static <T> String rawKey(IForgeRegistry<T> registry, int registryId) {
        if (!(registry instanceof ForgeRegistry<T> forgeRegistry)) {
            return null;
        }
        T value = forgeRegistry.getValue(registryId);
        if (value == null) {
            return null;
        }
        ResourceLocation key = registry.getKey(value);
        return key == null ? null : key.toString();
    }
}

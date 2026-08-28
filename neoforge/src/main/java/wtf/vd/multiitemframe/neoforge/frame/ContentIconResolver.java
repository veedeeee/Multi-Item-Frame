package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import wtf.vd.multiitemframe.frame.DisplayContentKind;
import wtf.vd.multiitemframe.neoforge.compat.mekanism.MekanismChemicalCompat;

/**
 * Resolves the atlas sprite + tint to draw for a non-{@code ITEM} slot content (Fluid/Chemical/
 * Energy - see {@link DisplayContentKind}), shared by both the in-world renderer
 * ({@code MultiItemFrameRenderer}) and the settings GUI ({@code MultiItemFrameScreen}).
 *
 * <p>Rather than shipping new custom icon art, this reuses whatever sprite the game already has
 * loaded for that exact Fluid/Chemical - the same still-texture/icon + tint that JEI's own
 * ingredient rendering samples - so the frame's display always matches the "real" in-game look of
 * that content, and never goes stale relative to resource pack / mod updates. All of these
 * sprites (fluids, Mekanism chemicals, and Mekanism's own energy bar icon) live in the vanilla
 * block atlas (see {@code MekanismRenderer#getSprite}).</p>
 */
public final class ContentIconResolver {

    /** Sprite location Mekanism itself uses for its energy bars/tanks (see
     *  {@code mekanism.client.render.MekanismRenderer#energyIcon}); referenced directly by
     *  location (not gated on Mekanism being loaded) since {@code TextureAtlas#apply} degrades
     *  gracefully to the "missing texture" sprite rather than throwing if it isn't stitched. */
    private static final ResourceLocation ENERGY_ICON = ResourceLocation.fromNamespaceAndPath("mekanism", "liquid/energy");

    private ContentIconResolver() {
    }

    public static TextureAtlasSprite getSprite(DisplayContentKind kind, String id) {
        ResourceLocation location = switch (kind) {
            case FLUID -> fluidStillTexture(id);
            case CHEMICAL -> ModList.get().isLoaded("mekanism") ? MekanismChemicalCompat.getIcon(id) : null;
            case ENERGY -> ENERGY_ICON;
            default -> null;
        };
        if (location == null) {
            return null;
        }
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(location);
    }

    /** ARGB tint to multiply the sprite by (opaque white/{@code 0xFFFFFFFF} = no tint). */
    public static int getTintARGB(DisplayContentKind kind, String id) {
        int rgb = switch (kind) {
            case FLUID -> fluidTint(id);
            case CHEMICAL -> ModList.get().isLoaded("mekanism") ? MekanismChemicalCompat.getTint(id) : 0xFFFFFF;
            default -> 0xFFFFFF;
        };
        return 0xFF000000 | rgb;
    }

    private static ResourceLocation fluidStillTexture(String id) {
        Fluid fluid = resolveFluid(id);
        return fluid == null ? null : IClientFluidTypeExtensions.of(fluid).getStillTexture();
    }

    private static int fluidTint(String id) {
        Fluid fluid = resolveFluid(id);
        return fluid == null ? 0xFFFFFF : IClientFluidTypeExtensions.of(fluid).getTintColor() & 0xFFFFFF;
    }

    private static Fluid resolveFluid(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return null;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(rl);
        return fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY ? null : fluid;
    }
}

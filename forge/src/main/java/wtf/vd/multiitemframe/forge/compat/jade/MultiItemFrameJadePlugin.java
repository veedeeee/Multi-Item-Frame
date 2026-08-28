package wtf.vd.multiitemframe.forge.compat.jade;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.Identifiers;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.forge.compat.mekanism.MekanismChemicalCompat;
import wtf.vd.multiitemframe.forge.frame.ContentIconResolver;
import wtf.vd.multiitemframe.forge.frame.MultiItemFrameEntity;
import wtf.vd.multiitemframe.frame.DisplayContentKind;

/**
 * Jade integration (Ch.5/7): shows what each slot is currently displaying (icon + name, one line
 * per non-empty slot - item, or Fluid/Chemical/Energy content) when looking at a Multi Item
 * Frame. No dedicated Jade support existed before this - the only prior "Jade support" was
 * overriding {@code Entity#getName()} to include the frame's size (see
 * {@code MultiItemFrameEntity}), which only affects the tooltip's header, never the body.
 * Quantities are intentionally never shown (frame slots are ghost/display-only, not a real
 * inventory - see {@code MultiItemFrameMenu}'s class javadoc).
 *
 * <p>Since {@code MultiItemFrameEntity} implements {@code Container}, Jade's own built-in
 * {@code EntityItemStorageProvider} already renders one icon+"1x Name" row per non-empty
 * <em>item</em> slot automatically (tagged {@link Identifiers#UNIVERSAL_ITEM_STORAGE}) - but it
 * only sees the real {@code ItemStack}s, so Fluid/Chemical/Energy slots (whose displayed item is
 * always empty by design) are invisible to it. Rather than showing both that built-in row list
 * *and* a second, differently-styled list for the remaining kinds, this removes the built-in row
 * and replaces it with one unified list (all kinds, same icon+name look, no counts) - see
 * {@code ContentProvider#appendTooltip} below.</p>
 *
 * <p>Entirely client-side: the displayed content (item/kind/id) is already replicated to the
 * client via the entity's {@code SynchedEntityData} for rendering purposes, so no
 * {@code IServerDataProvider} round-trip is needed - {@link EntityAccessor#getEntity()} already
 * returns the client's up-to-date tracked copy.</p>
 */
@WailaPlugin
public final class MultiItemFrameJadePlugin implements IWailaPlugin {

    private static final ResourceLocation PROVIDER_UID = new ResourceLocation(MultiItemFrame.MOD_ID, "content");
    /** Gap between the row's icon and its name text, in px (~half a space's width). */
    private static final int ICON_TEXT_GAP = 2;

    @Override
    public void register(IWailaCommonRegistration registration) {
        // No server data provider needed (see class javadoc) - nothing to register here.
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(ContentProvider.INSTANCE, MultiItemFrameEntity.class);
    }

    private enum ContentProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof MultiItemFrameEntity frame)) {
                return;
            }
            // Replaced by the unified per-slot rows below (see class javadoc) - remove Jade's own
            // built-in item-only row so items don't get listed twice.
            tooltip.remove(Identifiers.UNIVERSAL_ITEM_STORAGE);
            IElementHelper helper = IElementHelper.get();
            for (int slot = 0; slot < frame.getContainerSize(); slot++) {
                List<IElement> row = describeSlot(frame, helper, slot);
                if (row != null) {
                    tooltip.add(row);
                }
            }
        }

        private static List<IElement> describeSlot(MultiItemFrameEntity frame, IElementHelper helper, int slot) {
            DisplayContentKind kind = frame.getContentKind(slot);
            String id = frame.getContentId(slot);
            if (kind == DisplayContentKind.ITEM) {
                ItemStack stack = frame.getItem(slot);
                return stack.isEmpty() ? null
                        : List.of(helper.smallItem(stack), helper.spacer(ICON_TEXT_GAP, 0), helper.text(stack.getHoverName()));
            }
            Component name = describeName(kind, id);
            if (name == null) {
                return null;
            }
            TextureAtlasSprite sprite = ContentIconResolver.getSprite(kind, id);
            if (sprite == null) {
                return List.of(helper.text(name));
            }
            return List.of(new SpriteElement(sprite, ContentIconResolver.getTintARGB(kind, id)),
                    helper.spacer(ICON_TEXT_GAP, 0), helper.text(name));
        }

        private static Component describeName(DisplayContentKind kind, String id) {
            return switch (kind) {
                case FLUID -> fluidName(id);
                case GAS, INFUSION, PIGMENT, SLURRY ->
                        ModList.get().isLoaded("mekanism") ? MekanismChemicalCompat.getName(kind, id) : null;
                case ENERGY -> Component.translatable("jade.multiitemframe.energy");
                default -> null;
            };
        }

        private static Component fluidName(String id) {
            if (id == null || id.isEmpty()) {
                return null;
            }
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) {
                return null;
            }
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
            return fluid == null || fluid == Fluids.EMPTY ? null : fluid.getFluidType().getDescription();
        }

        @Override
        public ResourceLocation getUid() {
            return PROVIDER_UID;
        }

        @Override
        public int getDefaultPriority() {
            // Run after the built-in item storage provider so #appendTooltip's removal above
            // reliably finds an already-added row to remove (see Mekanism's own
            // MekanismJadePlugin, which uses the same TAIL trick to remove built-in rows).
            return TooltipPosition.TAIL;
        }
    }

    /** Draws a {@link TextureAtlasSprite} (+ tint) inline in a tooltip row - the same atlas
     *  sprite {@link ContentIconResolver} resolves for the in-world renderer and settings GUI,
     *  reused here since Jade's own {@code IElementHelper} has no generic "arbitrary atlas
     *  sprite" icon factory (only whole {@code ItemStack}s/vanilla fluids - see
     *  {@code mekanism.common.integration.lookingat.jade.JadeTooltipRenderer} for the reference
     *  pattern this follows). Sized to the tooltip's text line height (mirroring
     *  {@code IElementHelper#smallItem}, which shrinks its item icon the same way) so the icon
     *  lines up with the name text instead of towering over it at a full 16px. */
    private static final class SpriteElement extends Element {

        private final TextureAtlasSprite sprite;
        private final int tintARGB;
        private final int size;

        SpriteElement(TextureAtlasSprite sprite, int tintARGB) {
            this.sprite = sprite;
            this.tintARGB = tintARGB;
            this.size = Math.max(1, net.minecraft.client.Minecraft.getInstance().font.lineHeight - 1);
        }

        @Override
        public Vec2 getSize() {
            return new Vec2(this.size, this.size);
        }

        @Override
        public void render(GuiGraphics guiGraphics, float x, float y, float maxX, float maxY) {
            float a = ((this.tintARGB >>> 24) & 0xFF) / 255.0F;
            float r = ((this.tintARGB >> 16) & 0xFF) / 255.0F;
            float g = ((this.tintARGB >> 8) & 0xFF) / 255.0F;
            float b = (this.tintARGB & 0xFF) / 255.0F;
            guiGraphics.blit((int) x, (int) y, 0, this.size, this.size, this.sprite, r, g, b, a);
        }
    }
}

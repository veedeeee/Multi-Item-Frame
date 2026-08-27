package wtf.vd.multiitemframe.neoforge.compat.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import wtf.vd.multiitemframe.MultiItemFrame;
import wtf.vd.multiitemframe.neoforge.compat.mekanism.MekanismChemicalCompat;
import wtf.vd.multiitemframe.neoforge.frame.MultiItemFrameEntity;
import wtf.vd.multiitemframe.frame.DisplayContentKind;

/**
 * Jade integration (Ch.5/7): shows what each slot is currently displaying (item name, or Fluid/
 * Chemical/Energy content) when looking at a Multi Item Frame. No dedicated Jade support existed
 * before this - the only prior "Jade support" was overriding {@code Entity#getName()} to include
 * the frame's size (see {@code MultiItemFrameEntity}), which only affects the tooltip's header,
 * never the body. Quantities are intentionally never shown (frame slots are ghost/display-only,
 * not a real inventory - see {@code MultiItemFrameMenu}'s class javadoc).
 *
 * <p>Entirely client-side: the displayed content (item/kind/id) is already replicated to the
 * client via the entity's {@code SynchedEntityData} for rendering purposes, so no
 * {@code IServerDataProvider} round-trip is needed - {@link EntityAccessor#getEntity()} already
 * returns the client's up-to-date tracked copy.</p>
 */
@WailaPlugin
public final class MultiItemFrameJadePlugin implements IWailaPlugin {

    private static final ResourceLocation PROVIDER_UID = ResourceLocation.fromNamespaceAndPath(MultiItemFrame.MOD_ID, "content");

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
            MutableComponent line = null;
            for (int slot = 0; slot < frame.getContainerSize(); slot++) {
                Component name = describeSlot(frame, slot);
                if (name == null) {
                    continue;
                }
                if (line == null) {
                    line = Component.empty().append(name);
                } else {
                    line.append(", ").append(name);
                }
            }
            if (line != null) {
                tooltip.add(line);
            }
        }

        private static Component describeSlot(MultiItemFrameEntity frame, int slot) {
            DisplayContentKind kind = frame.getContentKind(slot);
            String id = frame.getContentId(slot);
            return switch (kind) {
                case ITEM -> {
                    ItemStack stack = frame.getItem(slot);
                    yield stack.isEmpty() ? null : stack.getHoverName();
                }
                case FLUID -> fluidName(id);
                case CHEMICAL -> ModList.get().isLoaded("mekanism") ? MekanismChemicalCompat.getName(id) : null;
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
            Fluid fluid = BuiltInRegistries.FLUID.get(rl);
            return fluid == null || fluid == Fluids.EMPTY ? null : fluid.getFluidType().getDescription();
        }

        @Override
        public ResourceLocation getUid() {
            return PROVIDER_UID;
        }
    }
}

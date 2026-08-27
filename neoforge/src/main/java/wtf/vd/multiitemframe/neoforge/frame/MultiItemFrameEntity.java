package wtf.vd.multiitemframe.neoforge.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import wtf.vd.multiitemframe.frame.DisplayContentKind;
import wtf.vd.multiitemframe.frame.FrameSize;
import wtf.vd.multiitemframe.frame.HighlightMode;

/**
 * NeoForge (1.21.1) implementation of the Multi Item Frame entity.
 *
 * <p>Behaves like a vanilla {@code ItemFrame} for placement/survival purposes,
 * but stores up to {@link FrameSize#MAX_SLOTS} independent item slots (only
 * the first {@code frameSize.slotCount()} are usable) each with its own
 * highlight mode/color, plus a single frame-wide background visibility flag.
 * Right-clicking opens a {@link MultiItemFrameMenu} instead of directly
 * placing/rotating a single item like vanilla item frames do.</p>
 */
public class MultiItemFrameEntity extends HangingEntity implements Container, MenuProvider {

    private static final EntityDataAccessor<Integer> DATA_SIZE =
            SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BACKGROUND =
            SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.BOOLEAN);

    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<ItemStack>[] DATA_ITEMS = new EntityDataAccessor[FrameSize.MAX_SLOTS];
    private static final EntityDataAccessor<Byte>[] DATA_MODES = new EntityDataAccessor[FrameSize.MAX_SLOTS];
    private static final EntityDataAccessor<Byte>[] DATA_COLORS = new EntityDataAccessor[FrameSize.MAX_SLOTS];
    /** Which kind of content a slot is displaying (see {@link DisplayContentKind}); {@code ITEM}
     *  (the default) means {@link #DATA_ITEMS} holds the actual displayed item, any other value
     *  means the slot instead displays a Fluid/Chemical/Energy type identified by
     *  {@link #DATA_CONTENT_IDS} (empty for {@code ENERGY}, which has no per-type identity - see
     *  {@code compat.container.ContainerContentExtractor}). */
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Byte>[] DATA_CONTENT_KINDS = new EntityDataAccessor[FrameSize.MAX_SLOTS];
    /** Registry name (e.g. {@code "minecraft:water"}, {@code "mekanism:hydrogen"}) of the
     *  Fluid/Chemical a slot displays, empty string when not applicable. */
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<String>[] DATA_CONTENT_IDS = new EntityDataAccessor[FrameSize.MAX_SLOTS];

    static {
        for (int i = 0; i < FrameSize.MAX_SLOTS; i++) {
            DATA_ITEMS[i] = SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.ITEM_STACK);
            DATA_MODES[i] = SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.BYTE);
            DATA_COLORS[i] = SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.BYTE);
            DATA_CONTENT_KINDS[i] = SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.BYTE);
            DATA_CONTENT_IDS[i] = SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.STRING);
        }
    }

    public MultiItemFrameEntity(EntityType<? extends MultiItemFrameEntity> entityType, Level level) {
        super(entityType, level);
    }

    public MultiItemFrameEntity(EntityType<? extends MultiItemFrameEntity> entityType, Level level, BlockPos pos,
            Direction direction, FrameSize frameSize) {
        super(entityType, level, pos);
        this.setDirection(direction);
        this.setFrameSize(frameSize);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, FrameSize.ONE_BY_ONE.ordinal());
        builder.define(DATA_BACKGROUND, true);
        for (int i = 0; i < FrameSize.MAX_SLOTS; i++) {
            builder.define(DATA_ITEMS[i], ItemStack.EMPTY);
            builder.define(DATA_MODES[i], (byte) HighlightMode.FRAME.ordinal());
            builder.define(DATA_COLORS[i], (byte) -1);
            builder.define(DATA_CONTENT_KINDS[i], (byte) DisplayContentKind.ITEM.ordinal());
            builder.define(DATA_CONTENT_IDS[i], "");
        }
    }

    public FrameSize getFrameSize() {
        return FrameSize.byOrdinalSafe(this.getEntityData().get(DATA_SIZE));
    }

    public void setFrameSize(FrameSize frameSize) {
        this.getEntityData().set(DATA_SIZE, frameSize.ordinal());
        this.recalculateBoundingBox();
    }

    public boolean isBackgroundVisible() {
        return this.getEntityData().get(DATA_BACKGROUND);
    }

    public void toggleBackground() {
        this.getEntityData().set(DATA_BACKGROUND, !this.isBackgroundVisible());
    }

    public HighlightMode getHighlightMode(int slot) {
        return HighlightMode.byOrdinalSafe(this.getEntityData().get(DATA_MODES[slot]));
    }

    public void cycleHighlightMode(int slot) {
        this.getEntityData().set(DATA_MODES[slot], (byte) this.getHighlightMode(slot).next().ordinal());
    }

    @Nullable
    public DyeColor getHighlightColor(int slot) {
        byte ordinal = this.getEntityData().get(DATA_COLORS[slot]);
        return ordinal < 0 ? null : DyeColor.byId(ordinal);
    }

    public void setHighlightColor(int slot, @Nullable DyeColor color) {
        this.getEntityData().set(DATA_COLORS[slot], (byte) (color == null ? -1 : color.getId()));
    }

    /** Which kind of content slot {@code slot} is displaying (see {@link DisplayContentKind}). */
    public DisplayContentKind getContentKind(int slot) {
        return DisplayContentKind.byOrdinalSafe(this.getEntityData().get(DATA_CONTENT_KINDS[slot]));
    }

    /** Registry name of the Fluid/Chemical displayed in {@code slot} (empty when not applicable). */
    public String getContentId(int slot) {
        return this.getEntityData().get(DATA_CONTENT_IDS[slot]);
    }

    /**
     * Sets a slot to display a non-item content type (Fluid/Chemical/Energy) - clears the slot's
     * displayed item, since only one of the two is shown at a time. Used by
     * {@code MultiItemFrameMenu#clicked} when the carried stack is a filled container (see
     * {@code compat.container.ContainerContentExtractor}); {@code id} should be empty for
     * {@link DisplayContentKind#ENERGY}, which has no per-type identity.
     */
    public void setDisplayContent(int slot, DisplayContentKind kind, String id) {
        this.getEntityData().set(DATA_ITEMS[slot], ItemStack.EMPTY);
        this.getEntityData().set(DATA_CONTENT_KINDS[slot], (byte) kind.ordinal());
        this.getEntityData().set(DATA_CONTENT_IDS[slot], id);
        this.setChanged();
    }

    // --- Container (slot storage backing the GUI) ---

    @Override
    public int getContainerSize() {
        return this.getFrameSize().slotCount();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!this.getItem(i).isEmpty() || this.getContentKind(i) != DisplayContentKind.ITEM) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.getEntityData().get(DATA_ITEMS[slot]);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = this.getItem(slot).copyWithCount(amount);
        this.setItem(slot, ItemStack.EMPTY);
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = this.getItem(slot);
        this.getEntityData().set(DATA_ITEMS[slot], ItemStack.EMPTY);
        this.getEntityData().set(DATA_CONTENT_KINDS[slot], (byte) DisplayContentKind.ITEM.ordinal());
        this.getEntityData().set(DATA_CONTENT_IDS[slot], "");
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            stack = stack.copyWithCount(1);
        }
        this.getEntityData().set(DATA_ITEMS[slot], stack);
        this.getEntityData().set(DATA_CONTENT_KINDS[slot], (byte) DisplayContentKind.ITEM.ordinal());
        this.getEntityData().set(DATA_CONTENT_IDS[slot], "");
        this.setChanged();
    }

    @Override
    public void setChanged() {
        if (this.pos != null) {
            this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < FrameSize.MAX_SLOTS; i++) {
            this.getEntityData().set(DATA_ITEMS[i], ItemStack.EMPTY);
            this.getEntityData().set(DATA_CONTENT_KINDS[i], (byte) DisplayContentKind.ITEM.ordinal());
            this.getEntityData().set(DATA_CONTENT_IDS[i], "");
        }
    }

    // --- MenuProvider (right click opens the settings GUI) ---

    @Override
    public Component getDisplayName() {
        return this.getFrameItemStack().getHoverName();
    }

    /**
     * {@code Entity#getName()} (distinct from the MenuProvider's {@link #getDisplayName()} above)
     * is what third-party tooltip mods like Jade read for an entity's displayed name - by default
     * it falls back to {@code EntityType#getDescription()}, which is always the generic "Multi
     * Item Frame"/"Glowing Multi Item Frame" translation since there's only one entity type per
     * glow variant (see {@code ModEntities} - FrameSize is synced entity data, not a separate
     * type), losing the size suffix (e.g. "1x1") that the item's own name has. Overriding this to
     * match {@link #getDisplayName()} makes Jade (and any other name reader) show the same
     * size-specific name the inventory/item tooltip already does.
     */
    @Override
    public Component getName() {
        return this.getFrameItemStack().getHoverName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MultiItemFrameMenu(containerId, playerInventory, this);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!this.level().isClientSide) {
            if (net.neoforged.fml.ModList.get().isLoaded("ae2")
                    && wtf.vd.multiitemframe.neoforge.compat.ae2.Ae2MemoryCardCompat.isMemoryCard(held)) {
                wtf.vd.multiitemframe.neoforge.compat.ae2.Ae2MemoryCardCompat.handle(this, player, held);
                return InteractionResult.CONSUME;
            }
            if (net.neoforged.fml.ModList.get().isLoaded("mekanism")
                    && wtf.vd.multiitemframe.neoforge.compat.mekanism.MekanismConfigCardCompat.isConfigCard(held)) {
                wtf.vd.multiitemframe.neoforge.compat.mekanism.MekanismConfigCardCompat.handle(this, player, held);
                return InteractionResult.CONSUME;
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.openMenu(this, buf -> buf.writeVarInt(this.getId()));
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    // --- Placement / survival (mirrors vanilla ItemFrame) ---

    // Always a fixed single-block footprint, exactly like vanilla ItemFrame - the multi-slot grid
    // (FrameSize.columns()/rows()) is a purely visual subdivision drawn within that one block's
    // face, not an actual multi-block placement (unlike vanilla Painting, which really does occupy
    // more than one block). Scaling this with FrameSize was a bug: it made placement require a
    // 2x2/1x2 clearance and perturbed the wall-flush centering math (which depends on width/height
    // via the axis-perpendicular size), causing non-1x1 frames to render at a shifted position
    // after being placed.
    // 0.75 (not 1.0) matches vanilla ItemFrame's 12/16 visual size and the renderer's
    // FOOTPRINT_HALF: without this, the interaction/collision box was a full 1x1 block, so -
    // unlike vanilla frames - the player could never click past the frame's corners to target the
    // block mounted behind it.
    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        float depth = 0.0625F;
        float footprint = 0.75F;
        Vec3 center = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        Direction.Axis axis = direction.getAxis();
        double sizeX = axis == Direction.Axis.X ? depth : footprint;
        double sizeY = axis == Direction.Axis.Y ? depth : footprint;
        double sizeZ = axis == Direction.Axis.Z ? depth : footprint;
        return AABB.ofSize(center, sizeX, sizeY, sizeZ);
    }

    @Override
    public boolean survives() {
        if (!this.level().noCollision(this)) {
            return false;
        }
        BlockState blockstate = this.level().getBlockState(this.pos.relative(this.direction.getOpposite()));
        boolean supported = blockstate.isSolid()
                || (this.direction.getAxis().isHorizontal() && DiodeBlock.isDiode(blockstate));
        return supported && this.level().getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();
    }

    @Override
    public void dropItem(@Nullable Entity brokenEntity) {
        this.playSound(this.getBreakSound(), 1.0F, 1.0F);
        if (!this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        boolean infiniteMaterials = brokenEntity instanceof Player player && player.hasInfiniteMaterials();
        if (!infiniteMaterials) {
            this.spawnAtLocation(this.getFrameItemStack());
        }
        // Displayed items are a selection, not real held stock (see MultiItemFrameMenu#clicked):
        // nothing was ever consumed to show them, so nothing extra drops here when the frame breaks.
        this.clearContent();
        this.gameEvent(GameEvent.BLOCK_CHANGE, brokenEntity);
    }

    protected ItemStack getFrameItemStack() {
        String itemId = this instanceof GlowMultiItemFrameEntity
                ? wtf.vd.multiitemframe.MultiItemFrame.glowFrameItemId(this.getFrameSize())
                : wtf.vd.multiitemframe.MultiItemFrame.frameItemId(this.getFrameSize());
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        wtf.vd.multiitemframe.MultiItemFrame.MOD_ID, itemId)));
    }

    @Override
    public void playPlacementSound() {
        this.playSound(this.getPlaceSound(), 1.0F, 1.0F);
    }

    public SoundEvent getPlaceSound() {
        return SoundEvents.ITEM_FRAME_PLACE;
    }

    public SoundEvent getBreakSound() {
        return SoundEvents.ITEM_FRAME_BREAK;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, this.direction.get3DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDirection(Direction.from3DDataValue(packet.getData()));
    }

    // --- Settings copy (shared contract for AE2 Memory Card / Mekanism Configuration Card, see ch.5) ---

    /** Snapshot of copyable settings (background + per-slot displayed item, highlight mode/color). */
    public CompoundTag copySettings() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowBackground", this.isBackgroundVisible());
        int slotCount = this.getFrameSize().slotCount();
        ListTag slotSettings = new ListTag();
        for (int i = 0; i < slotCount; i++) {
            CompoundTag slotTag = new CompoundTag();
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty()) {
                slotTag.put("Item", stack.save(this.registryAccess()));
            }
            slotTag.putByte("Mode", (byte) this.getHighlightMode(i).ordinal());
            DyeColor color = this.getHighlightColor(i);
            slotTag.putByte("Color", (byte) (color == null ? -1 : color.getId()));
            slotTag.putByte("ContentKind", (byte) this.getContentKind(i).ordinal());
            slotTag.putString("ContentId", this.getContentId(i));
            slotSettings.add(slotTag);
        }
        tag.put("Slots", slotSettings);
        return tag;
    }

    /**
     * Applies a snapshot produced by {@link #copySettings()} onto this frame. Slots are matched by
     * index - slot 0 is always the top-left-most slot and the last index is always the
     * bottom-right-most slot, in reading order (see {@link FrameSize#slotPosition}) - up to
     * however many slots both the source and this frame have in common: if the source frame had
     * fewer slots than this one, this frame's remaining slots are left untouched (not reset to
     * defaults); if the source had more, the extras are simply ignored.
     */
    public void pasteSettings(CompoundTag tag) {
        this.getEntityData().set(DATA_BACKGROUND, !tag.contains("ShowBackground") || tag.getBoolean("ShowBackground"));
        ListTag slotSettings = tag.getList("Slots", 10);
        int slotCount = Math.min(slotSettings.size(), this.getFrameSize().slotCount());
        for (int i = 0; i < slotCount; i++) {
            CompoundTag slotTag = slotSettings.getCompound(i);
            ItemStack stack = slotTag.contains("Item")
                    ? ItemStack.parse(this.registryAccess(), slotTag.getCompound("Item")).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
            this.setItem(i, stack);
            this.getEntityData().set(DATA_MODES[i], slotTag.getByte("Mode"));
            this.getEntityData().set(DATA_COLORS[i], slotTag.contains("Color") ? slotTag.getByte("Color") : (byte) -1);
            if (slotTag.contains("ContentKind")) {
                this.setDisplayContent(i, DisplayContentKind.byOrdinalSafe(slotTag.getByte("ContentKind")),
                        slotTag.getString("ContentId"));
            }
        }
    }

    // --- NBT persistence (1.21.1 Data Components API: ItemStack.save/parse need RegistryAccess) ---

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("FrameSize", this.getFrameSize().id());
        compound.putBoolean("ShowBackground", this.isBackgroundVisible());
        compound.putByte("Facing", (byte) this.direction.get3DDataValue());
        ListTag slots = new ListTag();
        for (int i = 0; i < this.getContainerSize(); i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("Slot", i);
            ItemStack stack = this.getItem(i);
            if (!stack.isEmpty()) {
                slotTag.put("Item", stack.save(this.registryAccess()));
            }
            slotTag.putByte("Mode", (byte) this.getHighlightMode(i).ordinal());
            DyeColor color = this.getHighlightColor(i);
            slotTag.putByte("Color", (byte) (color == null ? -1 : color.getId()));
            slotTag.putByte("ContentKind", (byte) this.getContentKind(i).ordinal());
            slotTag.putString("ContentId", this.getContentId(i));
            slots.add(slotTag);
        }
        compound.put("Slots", slots);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setFrameSize(compound.contains("FrameSize")
                ? FrameSize.byId(compound.getString("FrameSize"))
                : FrameSize.ONE_BY_ONE);
        this.getEntityData().set(DATA_BACKGROUND, !compound.contains("ShowBackground") || compound.getBoolean("ShowBackground"));
        this.setDirection(Direction.from3DDataValue(compound.getByte("Facing")));
        ListTag slots = compound.getList("Slots", 10);
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag slotTag = slots.getCompound(i);
            int slot = slotTag.getInt("Slot");
            if (slot < 0 || slot >= FrameSize.MAX_SLOTS) {
                continue;
            }
            ItemStack stack = slotTag.contains("Item")
                    ? ItemStack.parse(this.registryAccess(), slotTag.getCompound("Item")).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
            this.getEntityData().set(DATA_ITEMS[slot], stack);
            this.getEntityData().set(DATA_MODES[slot], slotTag.getByte("Mode"));
            this.getEntityData().set(DATA_COLORS[slot], slotTag.contains("Color") ? slotTag.getByte("Color") : (byte) -1);
            this.getEntityData().set(DATA_CONTENT_KINDS[slot], slotTag.contains("ContentKind")
                    ? slotTag.getByte("ContentKind") : (byte) DisplayContentKind.ITEM.ordinal());
            this.getEntityData().set(DATA_CONTENT_IDS[slot], slotTag.contains("ContentId") ? slotTag.getString("ContentId") : "");
        }
    }
}

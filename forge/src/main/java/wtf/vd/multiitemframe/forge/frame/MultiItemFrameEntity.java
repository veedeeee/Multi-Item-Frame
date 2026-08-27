package wtf.vd.multiitemframe.forge.frame;

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
import javax.annotation.Nullable;
import wtf.vd.multiitemframe.frame.FrameSize;
import wtf.vd.multiitemframe.frame.HighlightMode;

/**
 * Forge (1.20.1) implementation of the Multi Item Frame entity.
 *
 * <p>Mirrors the NeoForge (1.21.1) implementation of the same name, but uses the
 * pre-Data-Components 1.20.1 APIs: {@code HangingEntity} extends {@code Entity}
 * directly and exposes bounding-box size via {@code getWidth()}/{@code getHeight()}
 * (pixel units) instead of an overridable {@code calculateBoundingBox}, and
 * {@code ItemStack} NBT (de)serialization does not need a {@code RegistryAccess}.
 * See TASKS.md ch.2 notes for why this class cannot be shared with NeoForge.</p>
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

    static {
        for (int i = 0; i < FrameSize.MAX_SLOTS; i++) {
            DATA_ITEMS[i] = SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.ITEM_STACK);
            DATA_MODES[i] = SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.BYTE);
            DATA_COLORS[i] = SynchedEntityData.defineId(MultiItemFrameEntity.class, EntityDataSerializers.BYTE);
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
    protected void defineSynchedData() {
        this.getEntityData().define(DATA_SIZE, FrameSize.ONE_BY_ONE.ordinal());
        this.getEntityData().define(DATA_BACKGROUND, true);
        for (int i = 0; i < FrameSize.MAX_SLOTS; i++) {
            this.getEntityData().define(DATA_ITEMS[i], ItemStack.EMPTY);
            this.getEntityData().define(DATA_MODES[i], (byte) HighlightMode.FRAME.ordinal());
            this.getEntityData().define(DATA_COLORS[i], (byte) -1);
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

    // --- Bounding box size (1.20.1 uses pixel-unit getWidth()/getHeight(), unlike 1.21.1's calculateBoundingBox) ---

    // Always a fixed single-block footprint, exactly like vanilla ItemFrame - the multi-slot grid
    // (FrameSize.columns()/rows()) is a purely visual subdivision drawn within that one block's
    // face, not an actual multi-block placement (unlike vanilla Painting, which really does span
    // more than one block). Scaling this with FrameSize was a bug: it made placement require a 2x2/1x2
    // clearance and perturbed HangingEntity's wall-flush centering math (which depends on
    // width/height), causing non-1x1 frames to render at a shifted position after being placed.
    // 12 (not 16) matches vanilla ItemFrame's own getWidth()/getHeight() and the renderer's
    // FOOTPRINT_HALF (12/16 block visual size): without this, the interaction/collision box was a
    // full 1x1 block, so - unlike vanilla frames - the player could never click past the frame's
    // corners to target the block mounted behind it.
    @Override
    public int getWidth() {
        return 12;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    // --- Container (slot storage backing the GUI) ---

    @Override
    public int getContainerSize() {
        return this.getFrameSize().slotCount();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!this.getItem(i).isEmpty()) {
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
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            stack = stack.copyWithCount(1);
        }
        this.getEntityData().set(DATA_ITEMS[slot], stack);
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
        }
    }

    // --- MenuProvider (right click opens the settings GUI) ---

    @Override
    public Component getDisplayName() {
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
            if (net.minecraftforge.fml.ModList.get().isLoaded("ae2")
                    && wtf.vd.multiitemframe.forge.compat.ae2.Ae2MemoryCardCompat.isMemoryCard(held)) {
                wtf.vd.multiitemframe.forge.compat.ae2.Ae2MemoryCardCompat.handle(this, player, held);
                return InteractionResult.CONSUME;
            }
            if (net.minecraftforge.fml.ModList.get().isLoaded("mekanism")
                    && wtf.vd.multiitemframe.forge.compat.mekanism.MekanismConfigCardCompat.isConfigCard(held)) {
                wtf.vd.multiitemframe.forge.compat.mekanism.MekanismConfigCardCompat.handle(this, player, held);
                return InteractionResult.CONSUME;
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, this, buf -> buf.writeVarInt(this.getId()));
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    // --- Placement / survival (mirrors vanilla ItemFrame) ---

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
        boolean infiniteMaterials = brokenEntity instanceof Player player && player.getAbilities().instabuild;
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
        return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation(wtf.vd.multiitemframe.MultiItemFrame.MOD_ID, itemId)));
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
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
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
                slotTag.put("Item", stack.save(new CompoundTag()));
            }
            slotTag.putByte("Mode", (byte) this.getHighlightMode(i).ordinal());
            DyeColor color = this.getHighlightColor(i);
            slotTag.putByte("Color", (byte) (color == null ? -1 : color.getId()));
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
            ItemStack stack = slotTag.contains("Item") ? ItemStack.of(slotTag.getCompound("Item")) : ItemStack.EMPTY;
            this.setItem(i, stack);
            this.getEntityData().set(DATA_MODES[i], slotTag.getByte("Mode"));
            this.getEntityData().set(DATA_COLORS[i], slotTag.contains("Color") ? slotTag.getByte("Color") : (byte) -1);
        }
    }

    // --- NBT persistence (1.20.1: no RegistryAccess needed for ItemStack (de)serialization) ---

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
                slotTag.put("Item", stack.save(new CompoundTag()));
            }
            slotTag.putByte("Mode", (byte) this.getHighlightMode(i).ordinal());
            DyeColor color = this.getHighlightColor(i);
            slotTag.putByte("Color", (byte) (color == null ? -1 : color.getId()));
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
            ItemStack stack = slotTag.contains("Item") ? ItemStack.of(slotTag.getCompound("Item")) : ItemStack.EMPTY;
            this.getEntityData().set(DATA_ITEMS[slot], stack);
            this.getEntityData().set(DATA_MODES[slot], slotTag.getByte("Mode"));
            this.getEntityData().set(DATA_COLORS[slot], slotTag.contains("Color") ? slotTag.getByte("Color") : (byte) -1);
        }
    }
}

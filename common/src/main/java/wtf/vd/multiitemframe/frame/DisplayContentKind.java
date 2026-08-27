package wtf.vd.multiitemframe.frame;

/**
 * What kind of content a Multi Item Frame slot is displaying. {@link #ITEM} is the original,
 * default behavior (the slot's {@code ItemStack} is the thing being shown); the other values
 * are set when the player clicks a filled container (bucket, Mekanism gas/infusion/pigment/
 * slurry/chemical tank, or any FE-capable item) onto the slot instead of a plain item - in that
 * case the container's *content type* is displayed rather than the container item itself (see
 * {@code MultiItemFrameMenu#clicked}/the {@code compat.container} package). Pure enum, no
 * Minecraft API references, shared between loader modules.
 *
 * <p>{@link #GAS}/{@link #INFUSION}/{@link #PIGMENT}/{@link #SLURRY} are only used by the Forge
 * (1.20.1) module, whose Mekanism version still splits "chemicals" into 4 distinct types.
 * {@link #CHEMICAL} is only used by the NeoForge (1.21.1) module, whose Mekanism version unified
 * all 4 into a single generic chemical type/API.</p>
 */
public enum DisplayContentKind {
    ITEM,
    FLUID,
    GAS,
    INFUSION,
    PIGMENT,
    SLURRY,
    ENERGY,
    CHEMICAL;

    private static final DisplayContentKind[] VALUES = values();

    public static DisplayContentKind byOrdinalSafe(int ordinal) {
        return VALUES[Math.max(0, Math.min(VALUES.length - 1, ordinal))];
    }
}

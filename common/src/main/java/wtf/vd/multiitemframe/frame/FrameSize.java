package wtf.vd.multiitemframe.frame;

/**
 * Layout of a Multi Item Frame variant: how many display slots it has and how
 * they are arranged in a grid. Pure data, no Minecraft API references, so it
 * can be shared as-is between the Forge (1.20.1) and NeoForge (1.21.1)
 * loader modules.
 */
public enum FrameSize {

    ONE_BY_ONE("1x1", 1, 1, new double[][] { { 0, 0 } }),
    ONE_BY_TWO("1x2", 1, 2, new double[][] { { 0, 0 }, { 0, 1 } }),
    TWO_BY_ONE("2x1", 2, 1, new double[][] { { 0, 0 }, { 1, 0 } }),
    /** The lone "1"-side slot (index 0, top row) is centered across both columns. */
    ONE_AND_TWO("1and2", 2, 2, new double[][] { { 0.5, 0 }, { 0, 1 }, { 1, 1 } }),
    /** The lone "1"-side slot (index 2, bottom row) is centered across both columns. */
    TWO_AND_ONE("2and1", 2, 2, new double[][] { { 0, 0 }, { 1, 0 }, { 0.5, 1 } }),
    TWO_BY_TWO("2x2", 2, 2, new double[][] { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } });

    /** Maximum slot count across all sizes; used to size fixed-length synced data arrays. */
    public static final int MAX_SLOTS = 4;

    private final String id;
    private final int columns;
    private final int rows;
    private final double[][] slotPositions;

    FrameSize(String id, int columns, int rows, double[][] slotPositions) {
        this.id = id;
        this.columns = columns;
        this.rows = rows;
        this.slotPositions = slotPositions;
    }

    /** Item/recipe id suffix, e.g. {@code "1x1"} for {@code multiitemframe:frame_1x1}. */
    public String id() {
        return id;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    /**
     * Number of grid columns a slot's settings-GUI widget group should span when centering it
     * (used because the settings GUI always lays out slots within a fixed 2x2 grid region so
     * every {@link FrameSize} produces a same-sized panel): stretches to fill both columns when
     * this frame only has one column of its own, otherwise occupies a single column.
     */
    public int columnSpan() {
        return columns == 1 ? 2 : 1;
    }

    /** Row-axis counterpart of {@link #columnSpan()}. */
    public int rowSpan() {
        return rows == 1 ? 2 : 1;
    }

    public int slotCount() {
        return slotPositions.length;
    }

    /**
     * Column/row (x, y) of the given slot index within this frame's grid, as fractions of a
     * cell (usually whole numbers, but a lone slot on the "1" side of {@link #ONE_AND_TWO}/
     * {@link #TWO_AND_ONE} uses a {@code 0.5} column to sit centered across both columns).
     */
    public double[] slotPosition(int slotIndex) {
        return slotPositions[slotIndex];
    }

    public static FrameSize byId(String id) {
        for (FrameSize size : values()) {
            if (size.id.equals(id)) {
                return size;
            }
        }
        throw new IllegalArgumentException("Unknown FrameSize id: " + id);
    }

    public static FrameSize byOrdinalSafe(int ordinal) {
        FrameSize[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}

package wtf.vd.multiitemframe.frame;

/**
 * Highlight rendering mode for a single Multi Item Frame slot.
 * Pure enum, no Minecraft API references, shared between loader modules.
 */
public enum HighlightMode {
    FRAME,
    FILL;

    private static final HighlightMode[] VALUES = values();

    /** Toggles FRAME &lt;-&gt; FILL. "No highlight" is expressed by the slot having no {@code DyeColor} set. */
    public HighlightMode next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public static HighlightMode byOrdinalSafe(int ordinal) {
        return VALUES[Math.max(0, Math.min(VALUES.length - 1, ordinal))];
    }
}

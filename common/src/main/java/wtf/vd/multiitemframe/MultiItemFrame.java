package wtf.vd.multiitemframe;

import wtf.vd.multiitemframe.frame.FrameSize;

/** Shared constants for the mod. */
public final class MultiItemFrame {

    public static final String MOD_ID = "multiitemframe";

    /** Entity registry name for the non-glowing variant (size is synced entity data, see {@link FrameSize}). */
    public static final String ENTITY_FRAME = "multi_item_frame";

    /** Entity registry name for the glowing variant. */
    public static final String ENTITY_GLOW_FRAME = "glow_multi_item_frame";

    private MultiItemFrame() {
    }

    /** Item registry name for the non-glowing item of the given size, e.g. {@code "frame_1x1"}. */
    public static String frameItemId(FrameSize size) {
        return "frame_" + size.id();
    }

    /** Item registry name for the glowing item of the given size, e.g. {@code "glow_frame_1x1"}. */
    public static String glowFrameItemId(FrameSize size) {
        return "glow_frame_" + size.id();
    }

    /** Stable common bootstrap entrypoint for future cross-loader registrations. */
    public static void init() {
    }
}

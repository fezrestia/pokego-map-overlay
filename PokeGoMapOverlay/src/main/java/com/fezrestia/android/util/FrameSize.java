package com.fezrestia.android.util;

/**
 * Frame size descriptor.
 */
public class FrameSize {
    /**
     * Width.
     */
    @SuppressWarnings("WeakerAccess")
    public final int width;

    /**
     * Height.
     */
    @SuppressWarnings("WeakerAccess")
    public final int height;

    /**
     * CONSTRUCTOR.
     *
     * @param width Screen width.
     * @param height Screen height.
     */
    public FrameSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "FrameSize=" + width + 'x' + height;
    }

    /**
     * Get longer edge line length.
     *
     * @return Length
     */
    public int getLongLineSize() {
        return Math.max(width, height);
    }

    /**
     * Get shorter edge line length.
     *
     * @return Length
     */
    public int getShortLineSize() {
        return Math.min(width, height);
    }
}

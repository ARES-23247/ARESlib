package org.areslib.hardware.interfaces;

/**
 * Hardware-agnostic interface for an RGB-A color sensor (e.g., REV Color Sensor v3).
 */
public interface AresColorSensor {
    /** @return the red channel intensity. */
    int getRed();
    /** @return the green channel intensity. */
    int getGreen();
    /** @return the blue channel intensity. */
    int getBlue();
    /** @return the alpha (clear) channel intensity or overall luminescence. */
    int getAlpha();
}

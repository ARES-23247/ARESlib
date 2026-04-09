package org.areslib.hardware.interfaces;

/** Hardware-agnostic interface for an RGB-A color sensor (e.g., REV Color Sensor v3). */
public interface AresColorSensor {
  /**
   * Returns the red channel intensity.
   *
   * @return the red channel intensity.
   */
  int getRed();

  /**
   * Returns the green channel intensity.
   *
   * @return the green channel intensity.
   */
  int getGreen();

  /**
   * Returns the blue channel intensity.
   *
   * @return the blue channel intensity.
   */
  int getBlue();

  /**
   * Returns the alpha (clear) channel intensity or overall luminescence.
   *
   * @return the alpha (clear) channel intensity or overall luminescence.
   */
  int getAlpha();
}

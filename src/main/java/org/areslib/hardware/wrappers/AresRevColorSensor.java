package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.ColorSensor;
import org.areslib.hardware.interfaces.AresColorSensor;

/**
 * AresRevColorSensor standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * AresRevColorSensor}. Extracted and compiled as part of the ARESLib2 Code Audit for missing
 * documentation coverage.
 */
public class AresRevColorSensor implements AresColorSensor {
  private final ColorSensor colorSensor;

  private int cachedRed = 0;
  private int cachedGreen = 0;
  private int cachedBlue = 0;
  private int cachedAlpha = 0;
  private long lastReadTime = 0;
  private static final long CACHE_TIMEOUT_MS = 50;

  public AresRevColorSensor(ColorSensor colorSensor) {
    this.colorSensor = colorSensor;
  }

  private void updateCache() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastReadTime >= CACHE_TIMEOUT_MS) {
      cachedRed = colorSensor.red();
      cachedGreen = colorSensor.green();
      cachedBlue = colorSensor.blue();
      cachedAlpha = colorSensor.alpha();
      lastReadTime = currentTime;
    }
  }

  @Override
  public int getRed() {
    updateCache();
    return cachedRed;
  }

  @Override
  public int getGreen() {
    updateCache();
    return cachedGreen;
  }

  @Override
  public int getBlue() {
    updateCache();
    return cachedBlue;
  }

  @Override
  public int getAlpha() {
    updateCache();
    return cachedAlpha;
  }
}

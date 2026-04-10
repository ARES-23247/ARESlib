package org.areslib.hardware.wrappers;

import java.lang.reflect.Method;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.interfaces.AresColorSensor;

/**
 * AresSrsColorSensor standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * AresSrsColorSensor}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public class AresSrsColorSensor implements AresColorSensor {
  private final int port;
  private final Object srsHub;
  private Method getRedMethod;
  private Method getGreenMethod;
  private Method getBlueMethod;
  private Method getAlphaMethod;

  public AresSrsColorSensor(int port) {
    if (port < 0 || port > 2) {
      throw new IllegalArgumentException("SRS Hub color sensor port must be 0, 1, or 2.");
    }
    this.port = port;
    this.srsHub = AresHardwareManager.getActiveSrsHub();

    if (srsHub != null) {
      try {
        Class<?> clazz = srsHub.getClass();
        // Depending on the exact method names in the SRSHub SDK, map reflection bounds.
        // It usually mirrors standard V3 color metrics exactly.
        this.getRedMethod = clazz.getMethod("getColorRed", int.class);
        this.getGreenMethod = clazz.getMethod("getColorGreen", int.class);
        this.getBlueMethod = clazz.getMethod("getColorBlue", int.class);
        this.getAlphaMethod = clazz.getMethod("getColorAlpha", int.class);
      } catch (Exception ignored) {
        // Ignored
      }
    }
  }

  @Override
  public int getRed() {
    if (getRedMethod != null) {
      try {
        return ((Number) getRedMethod.invoke(srsHub, port)).intValue();
      } catch (Exception ignored) {
        // Ignored
      }
    }
    return 0;
  }

  @Override
  public int getGreen() {
    if (getGreenMethod != null) {
      try {
        return ((Number) getGreenMethod.invoke(srsHub, port)).intValue();
      } catch (Exception ignored) {
        // Ignored
      }
    }
    return 0;
  }

  @Override
  public int getBlue() {
    if (getBlueMethod != null) {
      try {
        return ((Number) getBlueMethod.invoke(srsHub, port)).intValue();
      } catch (Exception ignored) {
        // Ignored
      }
    }
    return 0;
  }

  @Override
  public int getAlpha() {
    if (getAlphaMethod != null) {
      try {
        return ((Number) getAlphaMethod.invoke(srsHub, port)).intValue();
      } catch (Exception ignored) {
        // Ignored
      }
    }
    return 0;
  }
}

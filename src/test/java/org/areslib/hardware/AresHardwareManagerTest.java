package org.areslib.hardware;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the load shedding math in {@link AresHardwareManager}. Validates the voltage-based
 * power scaling equations used to prevent robot brownouts.
 */
public class AresHardwareManagerTest {

  @Test
  void testInitialization() {}

  @Test
  public void testLoadSheddedLimitAtNominalVoltage() {
    double saved = AresHardwareManager.batteryVoltage;
    AresHardwareManager.batteryVoltage = 12.0;

    double result = AresHardwareManager.calculateLoadSheddedLimit(40.0, 5.0, 12.0, 8.0);
    assertEquals(40.0, result, 0.001, "At nominal voltage, limit should be maxCurrent");

    AresHardwareManager.batteryVoltage = saved;
  }

  @Test
  public void testLoadSheddedLimitAboveNominalVoltage() {
    double saved = AresHardwareManager.batteryVoltage;
    AresHardwareManager.batteryVoltage = 14.0;

    double result = AresHardwareManager.calculateLoadSheddedLimit(40.0, 5.0, 12.0, 8.0);
    assertEquals(40.0, result, 0.001);

    AresHardwareManager.batteryVoltage = saved;
  }

  @Test
  public void testLoadSheddedLimitAtCriticalVoltage() {
    double saved = AresHardwareManager.batteryVoltage;
    AresHardwareManager.batteryVoltage = 8.0;

    double result = AresHardwareManager.calculateLoadSheddedLimit(40.0, 5.0, 12.0, 8.0);
    assertEquals(5.0, result, 0.001, "At critical voltage, limit should be minCurrent");

    AresHardwareManager.batteryVoltage = saved;
  }

  @Test
  public void testLoadSheddedLimitBelowCriticalVoltage() {
    double saved = AresHardwareManager.batteryVoltage;
    AresHardwareManager.batteryVoltage = 6.0;

    double result = AresHardwareManager.calculateLoadSheddedLimit(40.0, 5.0, 12.0, 8.0);
    // Below critical: slope * (6.0 - 8.0) + 5.0 = negative offset, clamped to minCurrent
    assertEquals(5.0, result, 0.001, "Below critical should clamp to minCurrent");

    AresHardwareManager.batteryVoltage = saved;
  }

  @Test
  public void testLoadSheddedLimitMidpointVoltage() {
    double saved = AresHardwareManager.batteryVoltage;
    AresHardwareManager.batteryVoltage = 10.0; // Midpoint between 8.0 and 12.0

    double result = AresHardwareManager.calculateLoadSheddedLimit(40.0, 5.0, 12.0, 8.0);
    // slope = (40 - 5) / (12 - 8) = 8.75 A/V
    // limit = 5.0 + 8.75 * (10.0 - 8.0) = 5.0 + 17.5 = 22.5
    assertEquals(22.5, result, 0.001, "Midpoint voltage should produce linear interpolation");

    AresHardwareManager.batteryVoltage = saved;
  }

  @Test
  public void testLoadSheddedLimitMonotonicallyIncreases() {
    double saved = AresHardwareManager.batteryVoltage;

    double previous = 0.0;
    for (double v = 8.0; v <= 12.0; v += 0.5) {
      AresHardwareManager.batteryVoltage = v;
      double result = AresHardwareManager.calculateLoadSheddedLimit(40.0, 5.0, 12.0, 8.0);
      assertTrue(result >= previous, "Load shedded limit should increase with voltage");
      assertTrue(result >= 5.0, "Should never go below minCurrent");
      assertTrue(result <= 40.0, "Should never exceed maxCurrent");
      previous = result;
    }

    AresHardwareManager.batteryVoltage = saved;
  }
}

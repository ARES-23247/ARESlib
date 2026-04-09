package org.areslib.hmi;

import org.areslib.command.SubsystemBase;
import org.areslib.faults.AresFaultManager;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.wrappers.AresGamepad;

/**
 * Subsystem handling driver and operator input, including intelligent rumble feedback for faults
 * and voltage sags.
 */
public class AresOperatorInterface extends SubsystemBase {
  private final AresGamepad controller;

  // Pulse parameters
  private boolean isPulsing = false;
  private int pulseCounter = 0;
  // FTC loops run roughly 50Hz natively or higher on modern hardware with photon.
  // We'll use 15 loops on / 15 loops off for a fast half-second total pulse
  private static final int PULSE_DURATION_LOOPS = 15;
  private static final int PULSE_INTERVAL_LOOPS = 15;

  public AresOperatorInterface(AresGamepad controller) {
    this.controller = controller;
  }

  public AresGamepad getController() {
    return controller;
  }

  @Override
  public void periodic() {
    // 1. Critical Fault Pulse Priority
    if (AresFaultManager.hasNewError()) {
      isPulsing = true;
      pulseCounter = 0;
    }

    if (isPulsing) {
      if (pulseCounter < PULSE_DURATION_LOOPS) {
        controller.rumble(1.0, 1.0, 50); // Continuous sequence
      } else if (pulseCounter >= PULSE_DURATION_LOOPS + PULSE_INTERVAL_LOOPS) {
        isPulsing = false;
      }
      pulseCounter++;
      return; // Skip voltage rumble if critical fault is pulsing
    }

    // 2. Voltage Droop Rumble
    double voltage = AresHardwareManager.getBatteryVoltage();
    double nominal = 12.0;
    double critical = 8.0;

    if (voltage > 0.0 && voltage < nominal) {
      double voltageRange = nominal - critical;
      double rumbleStrength = 1.0 - ((voltage - critical) / voltageRange);
      rumbleStrength = Math.max(0.0, Math.min(1.0, rumbleStrength));

      if (rumbleStrength > 0.1) {
        // Standard FTC periodic loop delay is ~20ms, rumble length of 30ms blends
        controller.rumble(rumbleStrength, rumbleStrength, 30);
      }
    }
  }
}

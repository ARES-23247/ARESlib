package org.areslib.hardware.interfaces;

/**
 * Hardware-agnostic interface for a motor. Ensures ARESlib can control standard REV motors or
 * custom controllers effortlessly.
 */
public interface AresMotor {

  /**
   * Command the motor using voltage mapping (-12.0 to 12.0).
   *
   * @param volts Target voltage.
   */
  void setVoltage(double volts);

  /**
   * Returns estimated or actual applied voltage.
   *
   * @return Estimated or actual applied voltage.
   */
  double getVoltage();

  /**
   * Enables or disables hardware current polling. WARNING: Enabling this incurs a ~2ms hardware I2C
   * penalty per loop! Use only for Auto-Homing or Diagnostics.
   *
   * @param enabled True to poll current from the physical motor controller.
   */
  void setCurrentPolling(boolean enabled);

  /**
   * Gets the current draw of the motor.
   *
   * @return Current in AMPS, or 0.0 if setCurrentPolling(true) has not been called.
   */
  double getCurrentAmps();
}

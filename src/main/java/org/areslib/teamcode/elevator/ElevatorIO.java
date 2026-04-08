package org.areslib.teamcode.elevator;

import org.areslib.telemetry.AresLoggableInputs;

/** Example hardware abstraction interface for an Elevator subsystem. */
public interface ElevatorIO {
  /** Data class containing all incoming sensor data from the elevator. */
  class ElevatorInputs implements AresLoggableInputs {
    /** The elevator's linear position in meters. */
    public double positionMeters = 0.0;

    /** The elevator's linear velocity in meters per second. */
    public double velocityMps = 0.0;

    /** The voltage currently applied to the motor. */
    public double appliedVolts = 0.0;

    /** The electrical current drawn by the motor in amps. */
    public double currentAmps = 0.0;
  }

  /**
   * Updates the inputs with the latest sensor data.
   *
   * @param inputs The inputs to update.
   */
  void updateInputs(ElevatorInputs inputs);

  /**
   * Set the voltage applied to the elevator motor.
   *
   * @param volts The voltage (usually -12.0 to 12.0)
   */
  void setVoltage(double volts);

  /**
   * Set the target position of the grabber servo.
   *
   * @param position Range 0.0 to 1.0.
   */
  void setGrabberServo(double position);

  /**
   * Enable or disable current polling.
   *
   * @param enabled true to enable, false otherwise
   */
  default void setCurrentPolling(boolean enabled) {}

  /** Reset the encoder. */
  default void resetEncoder() {}
}

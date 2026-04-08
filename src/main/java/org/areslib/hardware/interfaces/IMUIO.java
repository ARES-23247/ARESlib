package org.areslib.hardware.interfaces;

import org.areslib.telemetry.AresLoggableInputs;

/**
 * AdvantageKit-style IO abstraction for IMU units (Inertial Measurement Units). Can be implemented
 * by physical REV Hub IMUs, GoBilda Pinpoints, or simulated kinematics.
 */
public interface IMUIO {

  class IMUInputs implements AresLoggableInputs {
    /** True if the IMU is currently returning healthy data. */
    public boolean connected = false;

    /** Raw yaw tracking angle (heading) in radians. */
    public double yawRadians = 0.0;

    /** Raw pitch angle in radians. */
    public double pitchRadians = 0.0;

    /** Raw roll angle in radians. */
    public double rollRadians = 0.0;

    /** Rotational velocity around the Z axis (yaw) in radians per second. */
    public double yawVelocityRadPerSec = 0.0;

    /** Rotational velocity around the Y axis (pitch) in radians per second. */
    public double pitchVelocityRadPerSec = 0.0;

    /** Rotational velocity around the X axis (roll) in radians per second. */
    public double rollVelocityRadPerSec = 0.0;
  }

  /**
   * Updates the data structure with the latest values from the underlying hardware sensor or
   * simulation.
   *
   * @param inputs The IMUInputs object to be populated.
   */
  default void updateInputs(IMUInputs inputs) {}

  /** Re-zeroes the IMU heading to zero (or resets the sensor origin if supported). */
  default void resetYaw() {}
}

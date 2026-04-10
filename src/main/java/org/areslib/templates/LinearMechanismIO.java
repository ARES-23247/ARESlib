package org.areslib.templates;

/**
 * IO interface template for a linear mechanism (elevators, slides, lifts).
 *
 * <p>Students: Copy this file and its companion {@link LinearMechanismIOSim} into your teamcode
 * package. Create a {@code LinearMechanismIOReal} that maps to your actual motor and encoder.
 */
public interface LinearMechanismIO {

  /** Logged inputs for a linear mechanism. */
  class Inputs {
    /** Current position in meters. */
    public double positionMeters = 0.0;

    /** Current velocity in meters per second. */
    public double velocityMetersPerSec = 0.0;

    /** Applied motor voltage. */
    public double appliedVolts = 0.0;

    /** Motor stator current in amps. */
    public double currentAmps = 0.0;

    /** Whether the lower limit switch is triggered. */
    public boolean lowerLimitSwitch = false;

    /** Whether the upper limit switch is triggered. */
    public boolean upperLimitSwitch = false;
  }

  /**
   * Updates the sensor inputs.
   *
   * @param inputs The input struct to populate.
   */
  default void updateInputs(Inputs inputs) {}

  /**
   * Commands the mechanism to a given voltage.
   *
   * @param volts Voltage to apply (-12.0 to 12.0).
   */
  default void setVoltage(double volts) {}

  /**
   * Commands the mechanism to move to a target position using closed-loop control.
   *
   * @param positionMeters Target position in meters.
   */
  default void setPosition(double positionMeters) {}

  /** Stops the mechanism. */
  default void stop() {}

  /** Resets the encoder position to zero. */
  default void resetEncoder() {}
}

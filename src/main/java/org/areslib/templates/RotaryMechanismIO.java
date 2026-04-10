package org.areslib.templates;

/**
 * IO interface template for a rotary mechanism (arms, wrists, pivots, turrets).
 *
 * <p>Students: Copy this file and its companion {@link RotaryMechanismIOSim} into your teamcode
 * package. Create a {@code RotaryMechanismIOReal} that maps to your actual servo/motor.
 */
public interface RotaryMechanismIO {

  /** Logged inputs for a rotary mechanism. */
  class Inputs {
    /** Current angle in radians. */
    public double angleRadians = 0.0;

    /** Current angular velocity in radians per second. */
    public double angularVelocityRadPerSec = 0.0;

    /** Applied motor voltage. */
    public double appliedVolts = 0.0;

    /** Motor stator current in amps. */
    public double currentAmps = 0.0;

    /** Whether the mechanism is at its CW hard stop. */
    public boolean atCWLimit = false;

    /** Whether the mechanism is at its CCW hard stop. */
    public boolean atCCWLimit = false;
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
   * Commands the mechanism to rotate to a target angle using closed-loop control.
   *
   * @param angleRadians Target angle in radians.
   */
  default void setAngle(double angleRadians) {}

  /** Stops the mechanism. */
  default void stop() {}

  /** Resets the encoder angle to zero. */
  default void resetEncoder() {}
}

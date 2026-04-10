package org.areslib.templates;

/**
 * IO interface template for a flywheel mechanism (shooters, rollers, spinners).
 *
 * <p>Students: Copy this file and its companion {@link FlywheelIOSim} into your teamcode package.
 * Create a {@code FlywheelIOReal} implementation that talks to your actual motor controller.
 *
 * <p>This follows the AdvantageKit IO pattern: all sensor data flows through the {@link Inputs}
 * class, which is automatically logged by the framework.
 */
public interface FlywheelIO {

  /** Logged inputs for a flywheel mechanism. */
  class Inputs {
    /** Current flywheel velocity in rotations per second. */
    public double velocityRPS = 0.0;

    /** Applied motor voltage. */
    public double appliedVolts = 0.0;

    /** Motor stator current in amps. */
    public double currentAmps = 0.0;

    /** Motor temperature in Celsius. */
    public double temperatureCelsius = 0.0;
  }

  /**
   * Updates the sensor inputs. Called every loop by the subsystem.
   *
   * @param inputs The input struct to populate with fresh sensor data.
   */
  default void updateInputs(Inputs inputs) {}

  /**
   * Commands the flywheel to spin at a given voltage.
   *
   * @param volts Voltage to apply (-12.0 to 12.0).
   */
  default void setVoltage(double volts) {}

  /**
   * Commands the flywheel to spin at a target velocity using closed-loop control.
   *
   * @param velocityRPS Target velocity in rotations per second.
   */
  default void setVelocity(double velocityRPS) {}

  /** Stops the flywheel. */
  default void stop() {}
}

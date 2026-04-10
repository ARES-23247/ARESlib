package org.areslib.templates;

/**
 * Simulation implementation of {@link RotaryMechanismIO} for desktop testing.
 *
 * <p>Models an arm or wrist pivot with gravity compensation and configurable angular limits.
 */
public class RotaryMechanismIOSim implements RotaryMechanismIO {
  private double angleRadians = 0.0;
  private double angularVelocityRadPerSec = 0.0;
  private double appliedVolts = 0.0;

  private final double minAngleRadians;
  private final double maxAngleRadians;
  private final double kV; // rad/s per volt per second
  private final double kFriction;
  private final double gravityTorqueScale;

  private static final double LOOP_PERIOD_SECONDS = 0.02;

  /**
   * Creates a rotary mechanism simulation.
   *
   * @param minAngleRadians Minimum angular limit (hard stop).
   * @param maxAngleRadians Maximum angular limit (hard stop).
   * @param kV Angular motor response: rad/s gained per volt per second.
   * @param kFriction Angular friction coefficient.
   * @param gravityTorqueScale Gravity torque scaling (0 for horizontal, 1.0 for full arm gravity).
   */
  public RotaryMechanismIOSim(
      double minAngleRadians,
      double maxAngleRadians,
      double kV,
      double kFriction,
      double gravityTorqueScale) {
    this.minAngleRadians = minAngleRadians;
    this.maxAngleRadians = maxAngleRadians;
    this.kV = kV;
    this.kFriction = kFriction;
    this.gravityTorqueScale = gravityTorqueScale;
  }

  /** Default constructor for a typical FTC arm. */
  public RotaryMechanismIOSim() {
    this(-Math.PI / 4.0, Math.PI * 0.75, 6.0, 2.0, 2.0);
  }

  @Override
  public void updateInputs(Inputs inputs) {
    // Gravity applies a torque proportional to cos(angle) for a pivot arm
    double gravityTorque = -Math.cos(angleRadians) * gravityTorqueScale;
    double effectiveVolts = appliedVolts + gravityTorque;

    double accel = (effectiveVolts * kV) - (angularVelocityRadPerSec * kFriction);
    angularVelocityRadPerSec += accel * LOOP_PERIOD_SECONDS;
    angleRadians += angularVelocityRadPerSec * LOOP_PERIOD_SECONDS;

    // Enforce angular limits
    if (angleRadians <= minAngleRadians) {
      angleRadians = minAngleRadians;
      angularVelocityRadPerSec = Math.max(0.0, angularVelocityRadPerSec);
    }
    if (angleRadians >= maxAngleRadians) {
      angleRadians = maxAngleRadians;
      angularVelocityRadPerSec = Math.min(0.0, angularVelocityRadPerSec);
    }

    inputs.angleRadians = angleRadians;
    inputs.angularVelocityRadPerSec = angularVelocityRadPerSec;
    inputs.appliedVolts = appliedVolts;
    inputs.currentAmps = Math.abs(appliedVolts) * 2.0;
    inputs.atCWLimit = angleRadians <= minAngleRadians + 0.01;
    inputs.atCCWLimit = angleRadians >= maxAngleRadians - 0.01;
  }

  @Override
  public void setVoltage(double volts) {
    appliedVolts = Math.max(-12.0, Math.min(12.0, volts));
  }

  @Override
  public void setAngle(double angleRadians) {
    double error = angleRadians - this.angleRadians;
    double gravityFF = Math.cos(this.angleRadians) * gravityTorqueScale;
    setVoltage(error * 6.0 + gravityFF);
  }

  @Override
  public void stop() {
    appliedVolts = 0.0;
  }

  @Override
  public void resetEncoder() {
    angleRadians = 0.0;
    angularVelocityRadPerSec = 0.0;
  }
}

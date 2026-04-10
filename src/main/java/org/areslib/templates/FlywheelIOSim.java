package org.areslib.templates;

/**
 * Simulation implementation of {@link FlywheelIO} for desktop testing.
 *
 * <p>Uses a simple velocity integration model: V = V + (voltage * kV - V * kFriction) * dt.
 */
public class FlywheelIOSim implements FlywheelIO {
  private double velocityRPS = 0.0;
  private double appliedVolts = 0.0;

  /** Feedforward constant: rotations per second per volt. */
  private final double kV;

  /** Friction constant: velocity decay per second (0.0 = no friction). */
  private final double kFriction;

  private static final double LOOP_PERIOD_SECONDS = 0.02;

  /**
   * Creates a flywheel simulation with tunable motor/friction constants.
   *
   * @param kV Motor response: RPS gained per volt applied per second.
   * @param kFriction Friction coefficient: fraction of velocity lost per second.
   */
  public FlywheelIOSim(double kV, double kFriction) {
    this.kV = kV;
    this.kFriction = kFriction;
  }

  /** Default constructor with typical FTC flywheel values. */
  public FlywheelIOSim() {
    this(8.0, 0.8);
  }

  @Override
  public void updateInputs(Inputs inputs) {
    // Simple first-order motor model
    double accel = (appliedVolts * kV) - (velocityRPS * kFriction);
    velocityRPS += accel * LOOP_PERIOD_SECONDS;

    inputs.velocityRPS = velocityRPS;
    inputs.appliedVolts = appliedVolts;
    inputs.currentAmps = Math.abs(appliedVolts) * 2.5; // Rough approximation
    inputs.temperatureCelsius = 25.0;
  }

  @Override
  public void setVoltage(double volts) {
    appliedVolts = Math.max(-12.0, Math.min(12.0, volts));
  }

  @Override
  public void setVelocity(double velocityRPS) {
    // Simple P controller for velocity targeting
    double error = velocityRPS - this.velocityRPS;
    setVoltage(error * 0.5);
  }

  @Override
  public void stop() {
    appliedVolts = 0.0;
  }
}

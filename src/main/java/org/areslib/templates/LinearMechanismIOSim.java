package org.areslib.templates;

/**
 * Simulation implementation of {@link LinearMechanismIO} for desktop testing.
 *
 * <p>Models an elevator/slide with gravity, motor response, and configurable travel limits.
 */
public class LinearMechanismIOSim implements LinearMechanismIO {
  private double positionMeters = 0.0;
  private double velocityMetersPerSec = 0.0;
  private double appliedVolts = 0.0;

  private final double maxHeightMeters;
  private final double kV; // meters per second per volt
  private final double kFriction;
  private final double gravityCompensationVolts;

  private static final double LOOP_PERIOD_SECONDS = 0.02;

  /**
   * Creates a linear mechanism simulation.
   *
   * @param maxHeightMeters Maximum travel in meters.
   * @param kV Motor response: m/s gained per volt per second.
   * @param kFriction Friction coefficient.
   * @param gravityCompensationVolts Voltage needed to hold position against gravity (0 for
   *     horizontal mechanisms).
   */
  public LinearMechanismIOSim(
      double maxHeightMeters, double kV, double kFriction, double gravityCompensationVolts) {
    this.maxHeightMeters = maxHeightMeters;
    this.kV = kV;
    this.kFriction = kFriction;
    this.gravityCompensationVolts = gravityCompensationVolts;
  }

  /** Default constructor for a typical FTC slide mechanism. */
  public LinearMechanismIOSim() {
    this(0.6, 0.5, 3.0, 1.5);
  }

  @Override
  public void updateInputs(Inputs inputs) {
    double effectiveVolts = appliedVolts - gravityCompensationVolts;
    double accel = (effectiveVolts * kV) - (velocityMetersPerSec * kFriction);
    velocityMetersPerSec += accel * LOOP_PERIOD_SECONDS;
    positionMeters += velocityMetersPerSec * LOOP_PERIOD_SECONDS;

    // Enforce travel limits
    if (positionMeters <= 0.0) {
      positionMeters = 0.0;
      velocityMetersPerSec = Math.max(0.0, velocityMetersPerSec);
    }
    if (positionMeters >= maxHeightMeters) {
      positionMeters = maxHeightMeters;
      velocityMetersPerSec = Math.min(0.0, velocityMetersPerSec);
    }

    inputs.positionMeters = positionMeters;
    inputs.velocityMetersPerSec = velocityMetersPerSec;
    inputs.appliedVolts = appliedVolts;
    inputs.currentAmps = Math.abs(appliedVolts) * 3.0;
    inputs.lowerLimitSwitch = positionMeters <= 0.001;
    inputs.upperLimitSwitch = positionMeters >= maxHeightMeters - 0.001;
  }

  @Override
  public void setVoltage(double volts) {
    appliedVolts = Math.max(-12.0, Math.min(12.0, volts));
  }

  @Override
  public void setPosition(double positionMeters) {
    double error = positionMeters - this.positionMeters;
    setVoltage(error * 8.0 + gravityCompensationVolts);
  }

  @Override
  public void stop() {
    appliedVolts = gravityCompensationVolts;
  }

  @Override
  public void resetEncoder() {
    positionMeters = 0.0;
    velocityMetersPerSec = 0.0;
  }
}

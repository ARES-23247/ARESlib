package org.firstinspires.ftc.teamcode.subsystems.elevator;

/**
 * Simulated implementation of {@link ElevatorIO} for offline physics testing.
 *
 * <p>Models a vertical elevator with gravity. The gravity voltage constant should match the {@code
 * kG} feedforward used in the real subsystem so that PID gains tuned in simulation transfer to
 * hardware.
 */
public class ElevatorIOSim implements ElevatorIO {
  /**
   * Simulated velocity constant (meters/sec per volt of effective force). This approximates how
   * fast the elevator moves per volt above the gravity hold. Not directly linked to a real-hardware
   * constant — tune to match observed sim behavior.
   */
  private static final double KV = 0.5;

  /**
   * Voltage required to hold the elevator stationary against gravity. <b>Must match {@code
   * ElevatorConstants.kG}</b> so that PID gains tuned in simulation transfer to hardware without
   * re-tuning.
   */
  private static final double GRAVITY_VOLTS = 0.2;

  private double appliedVolts = 0.0;
  private double positionMeters = 0.0;
  private double velocityMps = 0.0;
  private double lastTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();

  @Override
  public void updateInputs(ElevatorIOInputs inputs) {
    double currentTime = org.areslib.core.AresTimer.getFPGATimestamp();
    double dt = currentTime - lastTimeSeconds;
    lastTimeSeconds = currentTime;

    // Subtract gravity load from applied voltage before computing velocity
    double effectiveVolts = appliedVolts - GRAVITY_VOLTS;
    velocityMps = effectiveVolts * KV;
    positionMeters += velocityMps * dt;

    // Ensure floor bounds
    if (positionMeters < 0) {
      positionMeters = 0.0;
      velocityMps = 0.0;
    }

    // Output to inputs structure
    inputs.positionMeters = positionMeters;
    inputs.velocityMetersPerSec = velocityMps;
    inputs.appliedVolts = appliedVolts;
    // Approximate current draw: ~2A per volt. This is a rough linear model
    // for telemetry visualization only — real motors have non-linear I/V curves.
    inputs.currentAmps[0] = Math.abs(appliedVolts * 2.0);
  }

  @Override
  public void setVoltage(double volts) {
    appliedVolts = Math.max(-12.0, Math.min(12.0, volts));
  }
}

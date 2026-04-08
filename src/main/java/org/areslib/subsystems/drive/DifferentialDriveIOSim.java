package org.areslib.subsystems.drive;

/**
 * Simulated implementation of {@link DifferentialDriveIO} for offline physics testing.
 *
 * <p>This class uses a simple first-order kinematic model to approximate motor velocities based on
 * commanded voltages, integrating those velocities to track simulated positional state.
 */
public class DifferentialDriveIOSim implements DifferentialDriveIO {
  private static final double DRIVE_KV = 3.0; // meters per sec per volt

  private double leftAppliedVolts = 0.0;
  private double rightAppliedVolts = 0.0;

  private double leftPositionMeters = 0.0;
  private double leftVelocityMps = 0.0;
  private double rightPositionMeters = 0.0;
  private double rightVelocityMps = 0.0;

  @Override
  public void updateInputs(DifferentialDriveInputs inputs) {
    // Integrate physics
    leftVelocityMps = leftAppliedVolts * DRIVE_KV;
    leftPositionMeters += leftVelocityMps * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

    rightVelocityMps = rightAppliedVolts * DRIVE_KV;
    rightPositionMeters += rightVelocityMps * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

    // Populate inputs
    inputs.leftPositionMeters = leftPositionMeters;
    inputs.leftVelocityMps = leftVelocityMps;
    inputs.rightPositionMeters = rightPositionMeters;
    inputs.rightVelocityMps = rightVelocityMps;
  }

  @Override
  public void setVoltages(double leftVolts, double rightVolts) {
    leftAppliedVolts = Math.max(-12.0, Math.min(12.0, leftVolts));
    rightAppliedVolts = Math.max(-12.0, Math.min(12.0, rightVolts));
  }
}

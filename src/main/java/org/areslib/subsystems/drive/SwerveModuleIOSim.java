package org.areslib.subsystems.drive;

import org.areslib.math.MathUtil;

/**
 * Simulated implementation of {@link SwerveModuleIO} for offline physics testing.
 *
 * <p>This class uses a simple first-order kinematic model to approximate motor velocities based on
 * commanded voltages, integrating those velocities to track simulated positional state. Heading
 * wrap-around is automatically handled via {@link MathUtil#angleModulus}.
 */
public class SwerveModuleIOSim implements SwerveModuleIO {
  /**
   * Simulated motor velocity gain for drive motors (meters/sec per volt). This is the reciprocal of
   * the drive feedforward Kv: {@code 1.0 / Config.driveKv}. The default driveKv is 2.5 V·s/m, so
   * DRIVE_KV = 1/2.5 = 0.4 m/s/V.
   *
   * <p><b>If you change {@code Config.driveKv}, update this value to match.</b> Ideally this would
   * be injected from Config, but the IO layer is intentionally decoupled from Config to keep the
   * hardware abstraction clean.
   */
  private static final double DRIVE_KV = 0.4; // = 1.0 / 2.5 (default driveKv)

  /**
   * Simulated motor velocity gain for turn motors (rad/s per volt). This determines how responsive
   * the steering axis is in simulation. Higher values = faster steering response.
   */
  private static final double TURN_KV = 5.0;

  private double driveAppliedVolts = 0.0;
  private double turnAppliedVolts = 0.0;

  private double drivePositionMeters = 0.0;
  private double driveVelocityMps = 0.0;

  private double turnAbsolutePositionRad = 0.0;
  private double turnVelocityRadPerSec = 0.0;

  @Override
  public void updateInputs(SwerveModuleInputs inputs) {
    // Integrate basic physics
    driveVelocityMps = driveAppliedVolts * DRIVE_KV;
    drivePositionMeters += driveVelocityMps * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

    turnVelocityRadPerSec = turnAppliedVolts * TURN_KV;
    turnAbsolutePositionRad += turnVelocityRadPerSec * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

    // Wrap turn position to [-pi, pi] — handles any magnitude, not just single overflow
    turnAbsolutePositionRad = MathUtil.angleModulus(turnAbsolutePositionRad);

    // Output to inputs structure
    inputs.drivePositionMeters = drivePositionMeters;
    inputs.driveVelocityMps = driveVelocityMps;
    inputs.turnAbsolutePositionRad = turnAbsolutePositionRad;
    inputs.turnVelocityRadPerSec = turnVelocityRadPerSec;
  }

  @Override
  public void setDriveVoltage(double volts) {
    driveAppliedVolts = Math.max(-12.0, Math.min(12.0, volts));
  }

  @Override
  public void setTurnVoltage(double volts) {
    turnAppliedVolts = Math.max(-12.0, Math.min(12.0, volts));
  }
}

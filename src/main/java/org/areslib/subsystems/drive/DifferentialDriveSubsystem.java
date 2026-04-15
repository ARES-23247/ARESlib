package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;
import org.areslib.math.controller.PIDController;
import org.areslib.math.controller.SimpleMotorFeedforward;
import org.areslib.math.filter.SlewRateLimiter;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.math.kinematics.DifferentialDriveKinematics;
import org.areslib.math.kinematics.DifferentialDriveWheelSpeeds;
import org.areslib.telemetry.AresAutoLogger;

/**
 * AdvantageKit-style Differential Drive Subsystem.
 *
 * <p>Hardened for zero-allocation in high-frequency loops.
 */
public class DifferentialDriveSubsystem extends SubsystemBase implements AresDrivetrain {

  private static final double ANGULAR_ACCEL_MULTIPLIER = 2.0;

  /** Configuration data class for the DifferentialDriveSubsystem. */
  public static class Config {
    /** Full trackwidth (left-to-right wheel distance) in meters. */
    public double trackwidthMeters = 0.6;

    /** Proportional gain for drive velocity PID. */
    public double driveKp = 1.0;

    /** Integral gain for drive velocity PID. */
    public double driveKi = 0.0;

    /** Derivative gain for drive velocity PID. */
    public double driveKd = 0.0;

    /** Static feedforward for drive motors (Volts). */
    public double driveKsVolts = 0.1;

    /** Velocity feedforward for drive motors (Volts / (m/s)). */
    public double driveKvVoltsPerMps = 2.5;

    /** Max linear acceleration (m/s²). 0 = no slew limiting. */
    public double maxAccelerationMps2 = 0.0;
  }

  private final DifferentialDriveIO io;
  private final DifferentialDriveIO.DifferentialDriveInputs inputs =
      new DifferentialDriveIO.DifferentialDriveInputs();

  private double commandedVxMps = 0.0;
  private double commandedOmegaRadPerSec = 0.0;

  private final DifferentialDriveKinematics kinematics;

  private final PIDController leftPid;
  private final PIDController rightPid;
  private final SimpleMotorFeedforward driveFeedforward;

  private final SlewRateLimiter fwdLimiter;
  private final SlewRateLimiter rotLimiter;

  // Pre-allocated caches to avoid per-loop heap allocations
  private final ChassisSpeeds targetChassisSpeeds = new ChassisSpeeds();
  private final ChassisSpeeds discreteChassisSpeeds = new ChassisSpeeds();
  private final DifferentialDriveWheelSpeeds cachedWheelSpeeds = new DifferentialDriveWheelSpeeds();
  private double lastDriveTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();

  /**
   * Constructs the DifferentialDriveSubsystem.
   *
   * @param io The unified Differential Hardware interface.
   * @param config The structural parameters mapping physics to motor outputs.
   */
  public DifferentialDriveSubsystem(DifferentialDriveIO io, Config config) {
    this.io = io;
    this.kinematics = new DifferentialDriveKinematics(config.trackwidthMeters);

    this.leftPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);
    this.rightPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);

    this.driveFeedforward =
        new SimpleMotorFeedforward(config.driveKsVolts, config.driveKvVoltsPerMps);

    if (config.maxAccelerationMps2 > 0.0) {
      this.fwdLimiter = new SlewRateLimiter(config.maxAccelerationMps2);
      this.rotLimiter = new SlewRateLimiter(config.maxAccelerationMps2 * ANGULAR_ACCEL_MULTIPLIER);
    } else {
      this.fwdLimiter = null;
      this.rotLimiter = null;
    }
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    AresAutoLogger.processInputs("DifferentialDrive", inputs);
  }

  @Override
  public double getCommandedVx() {
    return commandedVxMps;
  }

  @Override
  public double getCommandedVy() {
    return 0.0;
  }

  @Override
  public double getCommandedOmega() {
    return commandedOmegaRadPerSec;
  }

  public void driveFieldCentric(
      double vxMetersPerSec,
      double vyMetersPerSec,
      double omegaRadPerSec,
      Rotation2d robotHeading) {
    targetChassisSpeeds.fromFieldRelative(
        vxMetersPerSec, vyMetersPerSec, omegaRadPerSec, robotHeading);
    // Differential drives can only use forward (vx) and turn (omega); strafe (vy) is dropped
    drive(targetChassisSpeeds.vxMetersPerSecond, targetChassisSpeeds.omegaRadiansPerSecond);
  }

  public void drive(double forwardMetersPerSec, double turnRadPerSec) {
    double currentTime = org.areslib.core.AresTimer.getFPGATimestamp();
    double dt = currentTime - lastDriveTimeSeconds;
    lastDriveTimeSeconds = currentTime;

    if (fwdLimiter != null) {
      forwardMetersPerSec = fwdLimiter.calculate(forwardMetersPerSec);
      turnRadPerSec = rotLimiter.calculate(turnRadPerSec);
    }

    this.commandedVxMps = forwardMetersPerSec;
    this.commandedOmegaRadPerSec = turnRadPerSec;

    ChassisSpeeds.discretize(forwardMetersPerSec, 0.0, turnRadPerSec, dt, discreteChassisSpeeds);

    kinematics.toWheelSpeeds(discreteChassisSpeeds, cachedWheelSpeeds);

    double leftVolts =
        driveFeedforward.calculate(cachedWheelSpeeds.leftMetersPerSecond)
            + leftPid.calculate(inputs.leftVelocityMps, cachedWheelSpeeds.leftMetersPerSecond);

    double rightVolts =
        driveFeedforward.calculate(cachedWheelSpeeds.rightMetersPerSecond)
            + rightPid.calculate(inputs.rightVelocityMps, cachedWheelSpeeds.rightMetersPerSecond);

    io.setVoltages(leftVolts, rightVolts);
  }
}

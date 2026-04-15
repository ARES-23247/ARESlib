package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;
import org.areslib.math.controller.PIDController;
import org.areslib.math.controller.SimpleMotorFeedforward;
import org.areslib.math.filter.SlewRateLimiter;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.math.kinematics.MecanumDriveKinematics;
import org.areslib.math.kinematics.MecanumDriveWheelSpeeds;
import org.areslib.telemetry.AresAutoLogger;

/**
 * AdvantageKit-style Mecanum Drive Subsystem.
 *
 * <p>Hardened for zero-allocation in high-frequency loops.
 */
public class MecanumDriveSubsystem extends SubsystemBase implements AresDrivetrain {

  private static final double ANGULAR_ACCEL_MULTIPLIER = 2.0;

  /** Configuration data class for the MecanumDriveSubsystem. */
  public static class Config {
    /** Full wheelbase (front-to-back axle distance) in meters. */
    public double wheelbaseMeters = 0.3;

    /** Full trackwidth (left-to-right wheel distance) in meters. */
    public double trackwidthMeters = 0.3;

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

  private final MecanumDriveIO io;
  private final MecanumDriveIO.MecanumDriveInputs inputs = new MecanumDriveIO.MecanumDriveInputs();

  private double commandedVxMps = 0.0;
  private double commandedVyMps = 0.0;
  private double commandedOmegaRadPerSec = 0.0;

  private final MecanumDriveKinematics kinematics;

  private final PIDController frontLeftPid;
  private final PIDController frontRightPid;
  private final PIDController rearLeftPid;
  private final PIDController rearRightPid;
  private final SimpleMotorFeedforward driveFeedforward;

  private final SlewRateLimiter fwdLimiter;
  private final SlewRateLimiter strLimiter;
  private final SlewRateLimiter rotLimiter;

  // Pre-allocated caches to avoid per-loop heap allocations
  private final ChassisSpeeds targetChassisSpeeds = new ChassisSpeeds();
  private final ChassisSpeeds discreteChassisSpeeds = new ChassisSpeeds();
  private final MecanumDriveWheelSpeeds cachedWheelSpeeds = new MecanumDriveWheelSpeeds();
  private double lastDriveTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();

  /**
   * Constructs the MecanumDriveSubsystem.
   *
   * @param io The unified Mecanum Hardware interface.
   * @param config The structural parameters mapping physics to motor outputs.
   */
  public MecanumDriveSubsystem(MecanumDriveIO io, Config config) {
    this.io = io;

    double halfWidth = config.trackwidthMeters / 2.0;
    double halfBase = config.wheelbaseMeters / 2.0;
    this.kinematics =
        new MecanumDriveKinematics(
            new Translation2d(halfBase, halfWidth), // FL
            new Translation2d(halfBase, -halfWidth), // FR
            new Translation2d(-halfBase, halfWidth), // RL
            new Translation2d(-halfBase, -halfWidth) // RR
            );

    this.frontLeftPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);
    this.frontRightPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);
    this.rearLeftPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);
    this.rearRightPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);

    this.driveFeedforward =
        new SimpleMotorFeedforward(config.driveKsVolts, config.driveKvVoltsPerMps);

    if (config.maxAccelerationMps2 > 0.0) {
      this.fwdLimiter = new SlewRateLimiter(config.maxAccelerationMps2);
      this.strLimiter = new SlewRateLimiter(config.maxAccelerationMps2);
      this.rotLimiter = new SlewRateLimiter(config.maxAccelerationMps2 * ANGULAR_ACCEL_MULTIPLIER);
    } else {
      this.fwdLimiter = null;
      this.strLimiter = null;
      this.rotLimiter = null;
    }
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    AresAutoLogger.processInputs("MecanumDrive", inputs);
  }

  @Override
  public double getCommandedVx() {
    return commandedVxMps;
  }

  @Override
  public double getCommandedVy() {
    return commandedVyMps;
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
    drive(
        targetChassisSpeeds.vxMetersPerSecond,
        targetChassisSpeeds.vyMetersPerSecond,
        targetChassisSpeeds.omegaRadiansPerSecond);
  }

  public void drive(double forwardMetersPerSec, double strafeMetersPerSec, double turnRadPerSec) {
    double currentTime = org.areslib.core.AresTimer.getFPGATimestamp();
    double dt = currentTime - lastDriveTimeSeconds;
    lastDriveTimeSeconds = currentTime;

    if (fwdLimiter != null) {
      forwardMetersPerSec = fwdLimiter.calculate(forwardMetersPerSec);
      strafeMetersPerSec = strLimiter.calculate(strafeMetersPerSec);
      turnRadPerSec = rotLimiter.calculate(turnRadPerSec);
    }

    this.commandedVxMps = forwardMetersPerSec;
    this.commandedVyMps = strafeMetersPerSec;
    this.commandedOmegaRadPerSec = turnRadPerSec;

    ChassisSpeeds.discretize(
        forwardMetersPerSec, strafeMetersPerSec, turnRadPerSec, dt, discreteChassisSpeeds);

    kinematics.toWheelSpeeds(discreteChassisSpeeds, cachedWheelSpeeds);

    double flVolts =
        driveFeedforward.calculate(cachedWheelSpeeds.frontLeftMetersPerSecond)
            + frontLeftPid.calculate(
                inputs.frontLeftVelocityMps, cachedWheelSpeeds.frontLeftMetersPerSecond);

    double frVolts =
        driveFeedforward.calculate(cachedWheelSpeeds.frontRightMetersPerSecond)
            + frontRightPid.calculate(
                inputs.frontRightVelocityMps, cachedWheelSpeeds.frontRightMetersPerSecond);

    double rlVolts =
        driveFeedforward.calculate(cachedWheelSpeeds.rearLeftMetersPerSecond)
            + rearLeftPid.calculate(
                inputs.rearLeftVelocityMps, cachedWheelSpeeds.rearLeftMetersPerSecond);

    double rrVolts =
        driveFeedforward.calculate(cachedWheelSpeeds.rearRightMetersPerSecond)
            + rearRightPid.calculate(
                inputs.rearRightVelocityMps, cachedWheelSpeeds.rearRightMetersPerSecond);

    io.setVoltages(flVolts, frVolts, rlVolts, rrVolts);
  }
}

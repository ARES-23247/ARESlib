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
 * <p>Acts as the structural controller for handling physics logic across left and right sides.
 * Configuration is provided via the nested {@link Config} class to avoid cyclic dependencies.
 */
public class DifferentialDriveSubsystem extends SubsystemBase implements AresDrivetrain {

  /**
   * @see SwerveDriveSubsystem#ANGULAR_ACCEL_MULTIPLIER
   */
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

    /** Static feedforward for drive motors. */
    public double driveKs = 0.1;

    /** Velocity feedforward for drive motors. */
    public double driveKv = 2.5;

    /** Max linear acceleration (m/s²). 0 = no slew limiting. */
    public double maxAccelerationMps2 = 0.0;
  }

  private final DifferentialDriveIO io;
  private final DifferentialDriveIO.DifferentialDriveInputs inputs =
      new DifferentialDriveIO.DifferentialDriveInputs();

  private double commandedVx = 0.0;
  private double commandedOmega = 0.0;

  private final DifferentialDriveKinematics kinematics;

  private final PIDController leftPid;
  private final PIDController rightPid;
  private final SimpleMotorFeedforward driveFeedforward;

  private final SlewRateLimiter fwdLimiter;
  private final SlewRateLimiter rotLimiter;

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

    this.driveFeedforward = new SimpleMotorFeedforward(config.driveKs, config.driveKv);

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

  /**
   * @return The commanded X velocity in m/s.
   */
  @Override
  public double getCommandedVx() {
    return commandedVx;
  }

  /**
   * Always returns 0.0 — differential drives cannot strafe.
   *
   * @return 0.0
   */
  @Override
  public double getCommandedVy() {
    return 0.0;
  }

  /**
   * @return The commanded angular velocity in rad/s.
   */
  @Override
  public double getCommandedOmega() {
    return commandedOmega;
  }

  /**
   * Commands the differential drive to move in a field-centric manner.
   *
   * <p>Note: Differential drives cannot strafe. The Y velocity component from the field-relative
   * transform is silently discarded.
   *
   * @param vxMetersPerSec The X velocity (field relative) in m/s.
   * @param vyMetersPerSec The Y velocity (field relative) in m/s — discarded.
   * @param omegaRadPerSec The angular velocity in rad/s.
   * @param robotHeading The current robot heading.
   */
  public void driveFieldCentric(
      double vxMetersPerSec,
      double vyMetersPerSec,
      double omegaRadPerSec,
      Rotation2d robotHeading) {
    ChassisSpeeds speeds =
        ChassisSpeeds.fromFieldRelativeSpeeds(
            vxMetersPerSec, vyMetersPerSec, omegaRadPerSec, robotHeading);
    // Differential drives can only use forward (vx) and turn (omega); strafe (vy) is dropped
    drive(speeds.vxMetersPerSecond, speeds.omegaRadiansPerSecond);
  }

  /**
   * Commands the differential drive to move in a robot-centric manner.
   *
   * @param forwardMetersPerSec The forward velocity in m/s (X axis).
   * @param turnRadPerSec The angular velocity in rad/s.
   */
  public void drive(double forwardMetersPerSec, double turnRadPerSec) {
    if (fwdLimiter != null) {
      forwardMetersPerSec =
          fwdLimiter.calculate(forwardMetersPerSec, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
      turnRadPerSec =
          rotLimiter.calculate(turnRadPerSec, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
    }

    this.commandedVx = forwardMetersPerSec;
    this.commandedOmega = turnRadPerSec;

    ChassisSpeeds speeds = new ChassisSpeeds(forwardMetersPerSec, 0.0, turnRadPerSec);
    speeds = ChassisSpeeds.discretize(speeds, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);

    DifferentialDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(speeds);

    double leftVolts =
        driveFeedforward.calculate(wheelSpeeds.leftMetersPerSecond)
            + leftPid.calculate(inputs.leftVelocityMps, wheelSpeeds.leftMetersPerSecond);

    double rightVolts =
        driveFeedforward.calculate(wheelSpeeds.rightMetersPerSecond)
            + rightPid.calculate(inputs.rightVelocityMps, wheelSpeeds.rightMetersPerSecond);

    io.setVoltages(leftVolts, rightVolts);
  }
}

package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;
import org.areslib.core.simulation.AresPhysicsWorld;
import org.areslib.math.controller.PIDController;
import org.areslib.math.controller.SimpleMotorFeedforward;
import org.areslib.math.filter.SlewRateLimiter;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.math.kinematics.SwerveDriveKinematics;
import org.areslib.math.kinematics.SwerveModuleState;
import org.areslib.telemetry.AresAutoLogger;
import org.areslib.telemetry.AresTelemetry;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;

/**
 * SwerveDriveSubsystem standard implementation.
 *
 * <p>Acts as the structural controller for handling physics logic across four modules.
 *
 * <p>Hardened for zero-allocation in high-frequency loops.
 */
public class SwerveDriveSubsystem extends SubsystemBase implements AresDrivetrain {

  private static final double ANGULAR_ACCEL_MULTIPLIER = 2.0;

  /** Configuration data class for the SwerveDriveSubsystem. */
  public static class Config {
    /** Distance from center to wheels along X (meters). */
    public double trackWidthXMeters = 0.3;

    /** Distance from center to wheels along Y (meters). */
    public double trackWidthYMeters = 0.3;

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

    /** Proportional gain for turn position PID. */
    public double turnKp = 3.0;

    /** Integral gain for turn position PID. */
    public double turnKi = 0.0;

    /** Derivative gain for turn position PID. */
    public double turnKd = 0.0;

    /** Static feedforward for turn motors (Volts). */
    public double turnKsVolts = 0.2;

    /** Max linear acceleration (m/s²). 0 = no slew limiting. */
    public double maxAccelerationMps2 = 0.0;

    /** Maximum safe module speed (m/s). */
    public double maxModuleSpeedMps = 4.0;
  }

  private final SwerveModuleIO frontLeft;
  private final SwerveModuleIO frontRight;
  private final SwerveModuleIO backLeft;
  private final SwerveModuleIO backRight;

  private final SwerveModuleIO.SwerveModuleInputs flInputs =
      new SwerveModuleIO.SwerveModuleInputs();
  private final SwerveModuleIO.SwerveModuleInputs frInputs =
      new SwerveModuleIO.SwerveModuleInputs();
  private final SwerveModuleIO.SwerveModuleInputs blInputs =
      new SwerveModuleIO.SwerveModuleInputs();
  private final SwerveModuleIO.SwerveModuleInputs brInputs =
      new SwerveModuleIO.SwerveModuleInputs();

  private double commandedVxMps = 0.0;
  private double commandedVyMps = 0.0;
  private double commandedOmegaRadPerSec = 0.0;

  private final SwerveDriveKinematics kinematics;

  private final PIDController[] drivePids = new PIDController[4];
  private final PIDController[] turnPids = new PIDController[4];
  private final SimpleMotorFeedforward driveFeedforward;
  private final double maxSpeedMps;
  private final double turnKsVolts;

  private final SlewRateLimiter fwdLimiter;
  private final SlewRateLimiter strLimiter;
  private final SlewRateLimiter rotLimiter;

  // Pre-allocated caches to avoid per-loop heap allocations
  private final SwerveModuleState[] cachedActualStates = new SwerveModuleState[4];
  private final SwerveModuleState[] cachedTargetStates = new SwerveModuleState[4];
  private final SwerveModuleState[] cachedOptimizedStates = new SwerveModuleState[4];
  private final Rotation2d[] cachedModuleRotations = new Rotation2d[4];
  private final ChassisSpeeds targetChassisSpeeds = new ChassisSpeeds();
  private final ChassisSpeeds discreteChassisSpeeds = new ChassisSpeeds();

  private final SwerveModuleIO[] modules;
  private final SwerveModuleIO.SwerveModuleInputs[] inputsArray;

  private Body simChassis = null;
  private double lastDriveTimeSeconds;

  public SwerveDriveSubsystem(
      Config config,
      SwerveModuleIO frontLeft,
      SwerveModuleIO frontRight,
      SwerveModuleIO backLeft,
      SwerveModuleIO backRight) {
    this.frontLeft = frontLeft;
    this.frontRight = frontRight;
    this.backLeft = backLeft;
    this.backRight = backRight;

    this.modules = new SwerveModuleIO[] {frontLeft, frontRight, backLeft, backRight};
    this.inputsArray =
        new SwerveModuleIO.SwerveModuleInputs[] {flInputs, frInputs, blInputs, brInputs};

    for (int i = 0; i < 4; i++) {
      cachedActualStates[i] = new SwerveModuleState();
      cachedTargetStates[i] = new SwerveModuleState();
      cachedOptimizedStates[i] = new SwerveModuleState();
      cachedModuleRotations[i] = new Rotation2d();
    }

    this.maxSpeedMps = config.maxModuleSpeedMps;
    this.turnKsVolts = config.turnKsVolts;

    this.kinematics =
        new SwerveDriveKinematics(
            new Translation2d(config.trackWidthXMeters, config.trackWidthYMeters), // FL
            new Translation2d(config.trackWidthXMeters, -config.trackWidthYMeters), // FR
            new Translation2d(-config.trackWidthXMeters, config.trackWidthYMeters), // BL
            new Translation2d(-config.trackWidthXMeters, -config.trackWidthYMeters) // BR
            );

    this.driveFeedforward =
        new SimpleMotorFeedforward(config.driveKsVolts, config.driveKvVoltsPerMps);

    for (int i = 0; i < 4; i++) {
      drivePids[i] = new PIDController(config.driveKp, config.driveKi, config.driveKd);
      turnPids[i] = new PIDController(config.turnKp, config.turnKi, config.turnKd);
      turnPids[i].enableContinuousInput(-Math.PI, Math.PI);
    }

    if (config.maxAccelerationMps2 > 0.0) {
      this.fwdLimiter = new SlewRateLimiter(config.maxAccelerationMps2);
      this.strLimiter = new SlewRateLimiter(config.maxAccelerationMps2);
      this.rotLimiter = new SlewRateLimiter(config.maxAccelerationMps2 * ANGULAR_ACCEL_MULTIPLIER);
    } else {
      this.fwdLimiter = null;
      this.strLimiter = null;
      this.rotLimiter = null;
    }

    if (org.areslib.core.AresRobot.isSimulation()) {
      simChassis = new Body();
      BodyFixture fixture = simChassis.addFixture(Geometry.createRectangle(0.4572, 0.4572));
      fixture.setDensity(20.0);
      fixture.setFriction(0.5);
      simChassis.setMass(MassType.NORMAL);
      simChassis.setLinearDamping(0.9);
      simChassis.setAngularDamping(0.9);
      AresPhysicsWorld.getInstance().addBody(simChassis);
    }
  }

  @Override
  public void periodic() {
    frontLeft.updateInputs(flInputs);
    frontRight.updateInputs(frInputs);
    backLeft.updateInputs(blInputs);
    backRight.updateInputs(brInputs);

    AresAutoLogger.processInputs("Swerve/FrontLeft", flInputs);
    AresAutoLogger.processInputs("Swerve/FrontRight", frInputs);
    AresAutoLogger.processInputs("Swerve/BackLeft", blInputs);
    AresAutoLogger.processInputs("Swerve/BackRight", brInputs);

    for (int i = 0; i < 4; i++) {
      cachedModuleRotations[i].set(inputsArray[i].turnAbsolutePositionRad);
      cachedActualStates[i].speedMetersPerSecond = inputsArray[i].driveVelocityMps;
      cachedActualStates[i].angle.set(cachedModuleRotations[i]);
    }
    AresTelemetry.logSwerveStates("Robot/SwerveActual", cachedActualStates);
  }

  @Override
  public void simulationPeriodic() {
    if (simChassis != null) {
      simChassis.setLinearVelocity(commandedVxMps, commandedVyMps);
      simChassis.setAngularVelocity(commandedOmegaRadPerSec);
    }
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

    kinematics.toSwerveModuleStates(discreteChassisSpeeds, cachedTargetStates);
    SwerveDriveKinematics.desaturateWheelSpeeds(cachedTargetStates, maxSpeedMps);

    AresTelemetry.logSwerveStates("Robot/SwerveTarget", cachedTargetStates);

    for (int i = 0; i < 4; i++) {
      cachedModuleRotations[i].set(inputsArray[i].turnAbsolutePositionRad);
      SwerveModuleState.optimize(
          cachedTargetStates[i], cachedModuleRotations[i], cachedOptimizedStates[i]);

      double targetSpeedMps = cachedOptimizedStates[i].speedMetersPerSecond;
      double targetAngleRad = cachedOptimizedStates[i].angle.getRadians();

      if (Math.abs(targetSpeedMps) <= 0.01) {
        // Hold the last commanded angle (not the measured one) to prevent slow drift.
        // Elite pattern from Team 254/2910: measured == setpoint → zero error → only static FF
        // → uncontrolled drift. Holding commanded angle maintains active PID correction.
        // targetAngleRad is already from cachedOptimizedStates[i], which is correct.
        targetSpeedMps = 0.0;
      }

      double feedforwardVolts = driveFeedforward.calculate(targetSpeedMps);
      double drivePidOut = drivePids[i].calculate(inputsArray[i].driveVelocityMps, targetSpeedMps);
      double turnPidOut =
          turnPids[i].calculate(inputsArray[i].turnAbsolutePositionRad, targetAngleRad);

      double turnFFOut = 0.0;
      if (Math.abs(turnPidOut) > 0.001) {
        turnFFOut = Math.signum(turnPidOut) * turnKsVolts;
      }

      modules[i].setDriveVoltage(feedforwardVolts + drivePidOut);
      modules[i].setTurnVoltage(turnPidOut + turnFFOut);
    }
  }
}

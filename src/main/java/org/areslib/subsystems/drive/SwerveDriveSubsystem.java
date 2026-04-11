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
 * AdvantageKit-style Swerve Drive Subsystem.
 *
 * <p>Acts as the structural controller for handling physics logic across four modules. This
 * subsystem is fully self-contained within the ARESLib library — it accepts primitive configuration
 * values via its {@link Config} inner class so that team-specific config classes do not leak into
 * the library namespace.
 */
public class SwerveDriveSubsystem extends SubsystemBase implements AresDrivetrain {

  /**
   * Angular acceleration is allowed to be faster than linear acceleration because rotational
   * inertia is significantly lower than translational inertia for typical FTC-sized robots. This
   * multiplier is applied to {@code maxAccelerationMps2} when constructing the rotation slew rate
   * limiter.
   */
  private static final double ANGULAR_ACCEL_MULTIPLIER = 2.0;

  /**
   * Configuration data class for the SwerveDriveSubsystem.
   *
   * <p>Contains physics and tuning parameters specific to a robot's physical construction. Team
   * code should populate an instance of this class and pass it to the subsystem constructor.
   */
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

    /** Static feedforward for drive motors. */
    public double driveKs = 0.1;

    /** Velocity feedforward for drive motors. */
    public double driveKv = 2.5;

    /** Proportional gain for turn position PID. */
    public double turnKp = 3.0;

    /** Integral gain for turn position PID. */
    public double turnKi = 0.0;

    /** Derivative gain for turn position PID. */
    public double turnKd = 0.0;

    /** Static feedforward for turn motors. */
    public double turnKs = 0.2;

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

  private double commandedVx = 0.0;
  private double commandedVy = 0.0;
  private double commandedOmega = 0.0;

  private final SwerveDriveKinematics kinematics;

  private final PIDController[] drivePids = new PIDController[4];
  private final PIDController[] turnPids = new PIDController[4];
  private final SimpleMotorFeedforward driveFeedforward;
  private final double maxSpeedMps;
  private final double turnKs;

  private final SlewRateLimiter fwdLimiter;
  private final SlewRateLimiter strLimiter;
  private final SlewRateLimiter rotLimiter;

  // Pre-allocated caches to avoid per-loop heap allocations in periodic() and drive()
  private final SwerveModuleState[] cachedActualStates = new SwerveModuleState[4];
  private final SwerveModuleState[] cachedOptimizedStates = new SwerveModuleState[4];
  private final Rotation2d[] cachedModuleRotations = new Rotation2d[4];
  private final SwerveModuleIO[] modules;
  private final SwerveModuleIO.SwerveModuleInputs[] inputsArray;

  private Body simChassis = null;

  /**
   * Constructs the SwerveDriveSubsystem.
   *
   * @param config The robot-specific physical tuning constants and constraints.
   * @param frontLeft The front left module IO.
   * @param frontRight The front right module IO.
   * @param backLeft The back left module IO.
   * @param backRight The back right module IO.
   */
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
      cachedOptimizedStates[i] = new SwerveModuleState();
      cachedModuleRotations[i] = new Rotation2d();
    }

    this.maxSpeedMps = config.maxModuleSpeedMps;
    this.turnKs = config.turnKs;

    this.kinematics =
        new SwerveDriveKinematics(
            new Translation2d(config.trackWidthXMeters, config.trackWidthYMeters), // FL
            new Translation2d(config.trackWidthXMeters, -config.trackWidthYMeters), // FR
            new Translation2d(-config.trackWidthXMeters, config.trackWidthYMeters), // BL
            new Translation2d(-config.trackWidthXMeters, -config.trackWidthYMeters) // BR
            );

    this.driveFeedforward = new SimpleMotorFeedforward(config.driveKs, config.driveKv);

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
      // 18x18 inches is 0.4572 meters
      BodyFixture fixture = simChassis.addFixture(Geometry.createRectangle(0.4572, 0.4572));
      fixture.setDensity(20.0); // Approximation for a ~15kg FTC robot
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

    SwerveModuleIO.SwerveModuleInputs[] allInputs = {flInputs, frInputs, blInputs, brInputs};
    for (int i = 0; i < 4; i++) {
      cachedModuleRotations[i] = new Rotation2d(allInputs[i].turnAbsolutePositionRad);
      cachedActualStates[i].speedMetersPerSecond = allInputs[i].driveVelocityMps;
      cachedActualStates[i].angle = cachedModuleRotations[i];
    }
    AresTelemetry.logSwerveStates("Robot/SwerveActual", cachedActualStates);
  }

  @Override
  public void simulationPeriodic() {
    if (simChassis != null) {
      // Apply commanded velocities to the physical body
      simChassis.setLinearVelocity(commandedVx, commandedVy);
      simChassis.setAngularVelocity(commandedOmega);
    }
  }

  /**
   * @return The commanded X velocity in m/s.
   */
  @Override
  public double getCommandedVx() {
    return commandedVx;
  }

  /**
   * @return The commanded Y velocity in m/s.
   */
  @Override
  public double getCommandedVy() {
    return commandedVy;
  }

  /**
   * @return The commanded angular velocity in rad/s.
   */
  @Override
  public double getCommandedOmega() {
    return commandedOmega;
  }

  /**
   * Commands the swerve drive to move in a field-centric manner.
   *
   * @param vxMetersPerSec The X velocity (field relative) in m/s.
   * @param vyMetersPerSec The Y velocity (field relative) in m/s.
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
    drive(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond);
  }

  /**
   * Commands the swerve drive to move in a robot-centric manner.
   *
   * @param forwardMetersPerSec The forward velocity in m/s (X axis).
   * @param strafeMetersPerSec The strafe velocity in m/s (Y axis).
   * @param turnRadPerSec The angular velocity in rad/s.
   */
  public void drive(double forwardMetersPerSec, double strafeMetersPerSec, double turnRadPerSec) {
    if (fwdLimiter != null) {
      forwardMetersPerSec =
          fwdLimiter.calculate(forwardMetersPerSec, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
      strafeMetersPerSec =
          strLimiter.calculate(strafeMetersPerSec, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
      turnRadPerSec =
          rotLimiter.calculate(turnRadPerSec, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
    }

    this.commandedVx = forwardMetersPerSec;
    this.commandedVy = strafeMetersPerSec;
    this.commandedOmega = turnRadPerSec;

    ChassisSpeeds speeds =
        new ChassisSpeeds(forwardMetersPerSec, strafeMetersPerSec, turnRadPerSec);
    speeds = ChassisSpeeds.discretize(speeds, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
    SwerveModuleState[] states = kinematics.toSwerveModuleStates(speeds);
    SwerveDriveKinematics.desaturateWheelSpeeds(states, maxSpeedMps);

    AresTelemetry.logSwerveStates("Robot/SwerveTarget", states);

    for (int i = 0; i < 4; i++) {
      cachedModuleRotations[i] = new Rotation2d(inputsArray[i].turnAbsolutePositionRad);
      SwerveModuleState.optimize(states[i], cachedModuleRotations[i], cachedOptimizedStates[i]);

      double targetSpeedMps = cachedOptimizedStates[i].speedMetersPerSecond;
      double targetAngleRad = cachedOptimizedStates[i].angle.getRadians();

      // Prevent snapping back to 0 degrees when stopping
      if (Math.abs(targetSpeedMps) <= 0.01) {
        targetAngleRad = inputsArray[i].turnAbsolutePositionRad;
        targetSpeedMps = 0.0;
      }

      double feedforwardVolts = driveFeedforward.calculate(targetSpeedMps);
      double drivePidOut = drivePids[i].calculate(inputsArray[i].driveVelocityMps, targetSpeedMps);

      double turnPidOut =
          turnPids[i].calculate(inputsArray[i].turnAbsolutePositionRad, targetAngleRad);

      // Apply static friction feedforward (Ks) in the direction of the PID output
      double turnFFOut = 0.0;
      if (Math.abs(turnPidOut) > 0.001) {
        turnFFOut = Math.signum(turnPidOut) * turnKs;
      }

      modules[i].setDriveVoltage(feedforwardVolts + drivePidOut);
      modules[i].setTurnVoltage(turnPidOut + turnFFOut);
    }
  }
}

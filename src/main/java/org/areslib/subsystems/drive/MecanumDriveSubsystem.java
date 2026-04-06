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
 * <p>
 * Acts as the structural controller for handling physics logic across all four wheels.
 * Configuration is provided via the nested {@link Config} class to avoid cyclic dependencies.
 */
public class MecanumDriveSubsystem extends SubsystemBase implements AresDrivetrain {

    /**
     * Configuration data class for the MecanumDriveSubsystem.
     */
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

        /** Static feedforward for drive motors. */
        public double driveKs = 0.1;
        /** Velocity feedforward for drive motors. */
        public double driveKv = 2.5;

        /** Max linear acceleration (m/s²). 0 = no slew limiting. */
        public double maxAccelerationMps2 = 0.0;
    }

    private final MecanumDriveIO io;
    private final MecanumDriveIO.MecanumDriveInputs inputs = new MecanumDriveIO.MecanumDriveInputs();

    private double commandedVx = 0.0;
    private double commandedVy = 0.0;
    private double commandedOmega = 0.0;

    private final MecanumDriveKinematics kinematics;

    private final PIDController frontLeftPid;
    private final PIDController frontRightPid;
    private final PIDController rearLeftPid;
    private final PIDController rearRightPid;
    private final SimpleMotorFeedforward driveFeedforward;

    private final SlewRateLimiter fwdLimiter;
    private final SlewRateLimiter strLimiter;
    private final SlewRateLimiter rotLimiter;

    /**
     * Constructs the MecanumDriveSubsystem.
     *
     * @param io     The unified Mecanum Hardware interface.
     * @param config The structural parameters mapping physics to motor outputs.
     */
    public MecanumDriveSubsystem(MecanumDriveIO io, Config config) {
        this.io = io;

        double halfWidth = config.trackwidthMeters / 2.0;
        double halfBase = config.wheelbaseMeters / 2.0;
        this.kinematics = new MecanumDriveKinematics(
            new Translation2d(halfBase, halfWidth),   // FL
            new Translation2d(halfBase, -halfWidth),  // FR
            new Translation2d(-halfBase, halfWidth),  // RL
            new Translation2d(-halfBase, -halfWidth)  // RR
        );

        this.frontLeftPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);
        this.frontRightPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);
        this.rearLeftPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);
        this.rearRightPid = new PIDController(config.driveKp, config.driveKi, config.driveKd);

        this.driveFeedforward = new SimpleMotorFeedforward(config.driveKs, config.driveKv);

        if (config.maxAccelerationMps2 > 0.0) {
            this.fwdLimiter = new SlewRateLimiter(config.maxAccelerationMps2);
            this.strLimiter = new SlewRateLimiter(config.maxAccelerationMps2);
            this.rotLimiter = new SlewRateLimiter(config.maxAccelerationMps2 * 2.0);
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

    /** @return The commanded X velocity in m/s. */
    @Override
    public double getCommandedVx() {
        return commandedVx;
    }

    /** @return The commanded Y velocity in m/s. */
    @Override
    public double getCommandedVy() {
        return commandedVy;
    }

    /** @return The commanded angular velocity in rad/s. */
    @Override
    public double getCommandedOmega() {
        return commandedOmega;
    }

    /**
     * Commands the mecanum drive to move in a field-centric manner.
     *
     * @param vxMetersPerSec  The X velocity (field relative) in m/s.
     * @param vyMetersPerSec  The Y velocity (field relative) in m/s.
     * @param omegaRadPerSec  The angular velocity in rad/s.
     * @param robotHeading    The current robot heading.
     */
    public void driveFieldCentric(double vxMetersPerSec, double vyMetersPerSec,
                                   double omegaRadPerSec, Rotation2d robotHeading) {
        ChassisSpeeds speeds = ChassisSpeeds.fromFieldRelativeSpeeds(
            vxMetersPerSec, vyMetersPerSec, omegaRadPerSec, robotHeading
        );
        drive(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond);
    }

    /**
     * Commands the mecanum drive to move in a robot-centric manner.
     *
     * @param forwardMetersPerSec The forward velocity in m/s (X axis).
     * @param strafeMetersPerSec  The strafe velocity in m/s (Y axis).
     * @param turnRadPerSec       The angular velocity in rad/s.
     */
    public void drive(double forwardMetersPerSec, double strafeMetersPerSec, double turnRadPerSec) {
        if (fwdLimiter != null) {
            forwardMetersPerSec = fwdLimiter.calculate(forwardMetersPerSec, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
            strafeMetersPerSec = strLimiter.calculate(strafeMetersPerSec, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
            turnRadPerSec = rotLimiter.calculate(turnRadPerSec, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
        }

        this.commandedVx = forwardMetersPerSec;
        this.commandedVy = strafeMetersPerSec;
        this.commandedOmega = turnRadPerSec;

        ChassisSpeeds speeds = new ChassisSpeeds(
            forwardMetersPerSec, strafeMetersPerSec, turnRadPerSec
        );
        speeds = ChassisSpeeds.discretize(speeds, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);

        MecanumDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(speeds);

        double flVolts = driveFeedforward.calculate(wheelSpeeds.frontLeftMetersPerSecond)
            + frontLeftPid.calculate(inputs.frontLeftVelocityMps, wheelSpeeds.frontLeftMetersPerSecond);

        double frVolts = driveFeedforward.calculate(wheelSpeeds.frontRightMetersPerSecond)
            + frontRightPid.calculate(inputs.frontRightVelocityMps, wheelSpeeds.frontRightMetersPerSecond);

        double rlVolts = driveFeedforward.calculate(wheelSpeeds.rearLeftMetersPerSecond)
            + rearLeftPid.calculate(inputs.rearLeftVelocityMps, wheelSpeeds.rearLeftMetersPerSecond);

        double rrVolts = driveFeedforward.calculate(wheelSpeeds.rearRightMetersPerSecond)
            + rearRightPid.calculate(inputs.rearRightVelocityMps, wheelSpeeds.rearRightMetersPerSecond);

        io.setVoltages(flVolts, frVolts, rlVolts, rrVolts);
    }
}

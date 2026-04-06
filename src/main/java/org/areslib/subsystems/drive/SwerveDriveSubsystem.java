package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;
import org.firstinspires.ftc.teamcode.subsystems.drive.SwerveConfig;
/**
 * AdvantageKit-style Swerve Drive Subsystem.
 * Acts as the structural controller for handling physics logic across four modules.
 */
public class SwerveDriveSubsystem extends SubsystemBase {

    private final SwerveModuleIO frontLeft;
    private final SwerveModuleIO frontRight;
    private final SwerveModuleIO backLeft;
    private final SwerveModuleIO backRight;

    private final SwerveModuleIO.SwerveModuleInputs flInputs = new SwerveModuleIO.SwerveModuleInputs();
    private final SwerveModuleIO.SwerveModuleInputs frInputs = new SwerveModuleIO.SwerveModuleInputs();
    private final SwerveModuleIO.SwerveModuleInputs blInputs = new SwerveModuleIO.SwerveModuleInputs();
    private final SwerveModuleIO.SwerveModuleInputs brInputs = new SwerveModuleIO.SwerveModuleInputs();

    private double commandedVx = 0.0;
    private double commandedVy = 0.0;
    private double commandedOmega = 0.0;

    // Ported WPILib kinematics
    private final org.areslib.math.kinematics.SwerveDriveKinematics kinematics;

    private final org.areslib.math.controller.PIDController[] drivePids = new org.areslib.math.controller.PIDController[4];
    private final org.areslib.math.controller.PIDController[] turnPids = new org.areslib.math.controller.PIDController[4];
    private final org.areslib.math.controller.SimpleMotorFeedforward driveFeedforward;
    private final double maxSpeedMps;
    private final double turnKs;

    private final org.areslib.math.filter.SlewRateLimiter fwdLimiter;
    private final org.areslib.math.filter.SlewRateLimiter strLimiter;
    private final org.areslib.math.filter.SlewRateLimiter rotLimiter;

    /**
     * Constructs the SwerveDriveSubsystem.
     * @param config The robot-specific physical tuning constants and constraints.
     * @param frontLeft The front left module IO.
     * @param frontRight The front right module IO.
     * @param backLeft The back left module IO.
     * @param backRight The back right module IO.
     */
    public SwerveDriveSubsystem(
            SwerveConfig config,
            SwerveModuleIO frontLeft, 
            SwerveModuleIO frontRight, 
            SwerveModuleIO backLeft, 
            SwerveModuleIO backRight) {
        this.frontLeft = frontLeft;
        this.frontRight = frontRight;
        this.backLeft = backLeft;
        this.backRight = backRight;

        this.maxSpeedMps = config.maxModuleSpeedMps;
        this.turnKs = config.turnKs;

        this.kinematics = new org.areslib.math.kinematics.SwerveDriveKinematics(
            new org.areslib.math.geometry.Translation2d(config.trackWidthXMeters, config.trackWidthYMeters),   // FL
            new org.areslib.math.geometry.Translation2d(config.trackWidthXMeters, -config.trackWidthYMeters),  // FR
            new org.areslib.math.geometry.Translation2d(-config.trackWidthXMeters, config.trackWidthYMeters),  // BL
            new org.areslib.math.geometry.Translation2d(-config.trackWidthXMeters, -config.trackWidthYMeters)  // BR
        );

        this.driveFeedforward = new org.areslib.math.controller.SimpleMotorFeedforward(config.driveKs, config.driveKv);

        for (int i = 0; i < 4; i++) {
            drivePids[i] = new org.areslib.math.controller.PIDController(config.driveKp, config.driveKi, config.driveKd);
            turnPids[i] = new org.areslib.math.controller.PIDController(config.turnKp, config.turnKi, config.turnKd);
            turnPids[i].enableContinuousInput(-Math.PI, Math.PI);
        }

        if (config.maxAccelerationMps2 > 0.0) {
            this.fwdLimiter = new org.areslib.math.filter.SlewRateLimiter(config.maxAccelerationMps2);
            this.strLimiter = new org.areslib.math.filter.SlewRateLimiter(config.maxAccelerationMps2);
            this.rotLimiter = new org.areslib.math.filter.SlewRateLimiter(config.maxAccelerationMps2 * 2.0); // Allow faster rotational accel
        } else {
            this.fwdLimiter = null;
            this.strLimiter = null;
            this.rotLimiter = null;
        }
    }

    @Override
    public void periodic() {
        frontLeft.updateInputs(flInputs);
        frontRight.updateInputs(frInputs);
        backLeft.updateInputs(blInputs);
        backRight.updateInputs(brInputs);

        org.areslib.telemetry.AresAutoLogger.processInputs("Swerve/FrontLeft", flInputs);
        org.areslib.telemetry.AresAutoLogger.processInputs("Swerve/FrontRight", frInputs);
        org.areslib.telemetry.AresAutoLogger.processInputs("Swerve/BackLeft", blInputs);
        org.areslib.telemetry.AresAutoLogger.processInputs("Swerve/BackRight", brInputs);

        org.areslib.math.kinematics.SwerveModuleState[] actualStates = new org.areslib.math.kinematics.SwerveModuleState[] {
            new org.areslib.math.kinematics.SwerveModuleState(flInputs.driveVelocityMps, new org.areslib.math.geometry.Rotation2d(flInputs.turnAbsolutePositionRad)),
            new org.areslib.math.kinematics.SwerveModuleState(frInputs.driveVelocityMps, new org.areslib.math.geometry.Rotation2d(frInputs.turnAbsolutePositionRad)),
            new org.areslib.math.kinematics.SwerveModuleState(blInputs.driveVelocityMps, new org.areslib.math.geometry.Rotation2d(blInputs.turnAbsolutePositionRad)),
            new org.areslib.math.kinematics.SwerveModuleState(brInputs.driveVelocityMps, new org.areslib.math.geometry.Rotation2d(brInputs.turnAbsolutePositionRad)),
        };
        org.areslib.telemetry.AresTelemetry.logSwerveStates("Robot/SwerveActual", actualStates);
    }

    /**
     * @return The commanded X velocity in m/s.
     */
    public double getCommandedVx() { return commandedVx; }

    /**
     * @return The commanded Y velocity in m/s.
     */
    public double getCommandedVy() { return commandedVy; }

    /**
     * @return The commanded angular velocity in rad/s.
     */
    public double getCommandedOmega() { return commandedOmega; }

    /**
     * Commands the swerve drive to move in a field-centric manner.
     * @param vxMetersPerSec The X velocity (field relative) in m/s.
     * @param vyMetersPerSec The Y velocity (field relative) in m/s.
     * @param omegaRadPerSec The angular velocity in rad/s.
     * @param robotHeading The current robot heading.
     */
    public void driveFieldCentric(double vxMetersPerSec, double vyMetersPerSec, double omegaRadPerSec, org.areslib.math.geometry.Rotation2d robotHeading) {
        org.areslib.math.kinematics.ChassisSpeeds speeds = org.areslib.math.kinematics.ChassisSpeeds.fromFieldRelativeSpeeds(
            vxMetersPerSec, vyMetersPerSec, omegaRadPerSec, robotHeading
        );
        drive(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond);
    }

    /**
     * Commands the swerve drive to move.
     * @param forwardMetersPerSec The forward velocity in m/s (X axis).
     * @param strafeMetersPerSec The strafe velocity in m/s (Y axis).
     * @param turnRadPerSec The angular velocity in rad/s.
     */
    public void drive(double forwardMetersPerSec, double strafeMetersPerSec, double turnRadPerSec) {
        if (fwdLimiter != null) {
            forwardMetersPerSec = fwdLimiter.calculate(forwardMetersPerSec, 0.02);
            strafeMetersPerSec = strLimiter.calculate(strafeMetersPerSec, 0.02);
            turnRadPerSec = rotLimiter.calculate(turnRadPerSec, 0.02);
        }

        this.commandedVx = forwardMetersPerSec;
        this.commandedVy = strafeMetersPerSec;
        this.commandedOmega = turnRadPerSec;
        
        org.areslib.math.kinematics.ChassisSpeeds speeds = new org.areslib.math.kinematics.ChassisSpeeds(
            forwardMetersPerSec, strafeMetersPerSec, turnRadPerSec
        );
        speeds = org.areslib.math.kinematics.ChassisSpeeds.discretize(speeds, 0.02);
        org.areslib.math.kinematics.SwerveModuleState[] states = kinematics.toSwerveModuleStates(speeds);
        org.areslib.math.kinematics.SwerveDriveKinematics.desaturateWheelSpeeds(states, maxSpeedMps);

        org.areslib.telemetry.AresTelemetry.logSwerveStates("Robot/SwerveTarget", states);

        SwerveModuleIO[] modules = {frontLeft, frontRight, backLeft, backRight};
        SwerveModuleIO.SwerveModuleInputs[] inputs = {flInputs, frInputs, blInputs, brInputs};

        for (int i = 0; i < 4; i++) {
            org.areslib.math.kinematics.SwerveModuleState optimizedState = org.areslib.math.kinematics.SwerveModuleState.optimize(
                states[i], 
                new org.areslib.math.geometry.Rotation2d(inputs[i].turnAbsolutePositionRad)
            );

            double targetSpeedMps = optimizedState.speedMetersPerSecond;
            double targetAngleRad = optimizedState.angle.getRadians();

            // Prevent snapping back to 0 degrees when stopping
            if (Math.abs(targetSpeedMps) <= 0.01) {
                targetAngleRad = inputs[i].turnAbsolutePositionRad;
                targetSpeedMps = 0.0;
            }

            double feedforwardVolts = driveFeedforward.calculate(targetSpeedMps);
            double drivePidOut = drivePids[i].calculate(inputs[i].driveVelocityMps, targetSpeedMps);
            
            double turnPidOut = turnPids[i].calculate(inputs[i].turnAbsolutePositionRad, targetAngleRad);
            
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

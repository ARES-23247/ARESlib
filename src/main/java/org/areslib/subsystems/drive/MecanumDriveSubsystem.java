package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;
import org.firstinspires.ftc.teamcode.subsystems.drive.MecanumConfig;

/**
 * AdvantageKit-style Mecanum Drive Subsystem.
 * Acts as the structural controller for handling physics logic across all four wheels.
 */
public class MecanumDriveSubsystem extends SubsystemBase implements AresDrivetrain {

    private final MecanumDriveIO io;
    private final MecanumDriveIO.MecanumDriveInputs inputs = new MecanumDriveIO.MecanumDriveInputs();

    private double commandedVx = 0.0;
    private double commandedVy = 0.0;
    private double commandedOmega = 0.0;

    // Ported WPILib kinematics
    private final org.areslib.math.kinematics.MecanumDriveKinematics kinematics;

    private final org.areslib.math.controller.PIDController frontLeftPid;
    private final org.areslib.math.controller.PIDController frontRightPid;
    private final org.areslib.math.controller.PIDController rearLeftPid;
    private final org.areslib.math.controller.PIDController rearRightPid;
    private final org.areslib.math.controller.SimpleMotorFeedforward driveFeedforward;

    private final org.areslib.math.filter.SlewRateLimiter fwdLimiter;
    private final org.areslib.math.filter.SlewRateLimiter strLimiter;
    private final org.areslib.math.filter.SlewRateLimiter rotLimiter;

    /**
     * Constructs the MecanumDriveSubsystem.
     * @param io The unified Mecanum Hardware interface.
     * @param config The structural parameters mapping physics to motor outputs.
     */
    public MecanumDriveSubsystem(MecanumDriveIO io, MecanumConfig config) {
        this.io = io;
        
        double halfWidth = config.trackwidthMeters / 2.0;
        double halfBase = config.wheelbaseMeters / 2.0;
        this.kinematics = new org.areslib.math.kinematics.MecanumDriveKinematics(
            new org.areslib.math.geometry.Translation2d(halfBase, halfWidth),   // FL
            new org.areslib.math.geometry.Translation2d(halfBase, -halfWidth),  // FR
            new org.areslib.math.geometry.Translation2d(-halfBase, halfWidth),  // RL
            new org.areslib.math.geometry.Translation2d(-halfBase, -halfWidth)  // RR
        );

        this.frontLeftPid = new org.areslib.math.controller.PIDController(config.driveKp, config.driveKi, config.driveKd);
        this.frontRightPid = new org.areslib.math.controller.PIDController(config.driveKp, config.driveKi, config.driveKd);
        this.rearLeftPid = new org.areslib.math.controller.PIDController(config.driveKp, config.driveKi, config.driveKd);
        this.rearRightPid = new org.areslib.math.controller.PIDController(config.driveKp, config.driveKi, config.driveKd);
        
        this.driveFeedforward = new org.areslib.math.controller.SimpleMotorFeedforward(config.driveKs, config.driveKv);

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
        io.updateInputs(inputs);
        org.areslib.telemetry.AresAutoLogger.processInputs("MecanumDrive", inputs);
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
     * Commands the mecanum drive to move in a field-centric manner.
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
     * Commands the mecanum drive to move.
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

        org.areslib.math.kinematics.MecanumDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(speeds);

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

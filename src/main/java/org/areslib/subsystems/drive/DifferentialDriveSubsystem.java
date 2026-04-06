package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;
import org.firstinspires.ftc.teamcode.subsystems.drive.DifferentialConfig;

/**
 * AdvantageKit-style Differential Drive Subsystem.
 * Acts as the structural controller for handling physics logic across left and right sides.
 */
public class DifferentialDriveSubsystem extends SubsystemBase implements AresDrivetrain {

    private final DifferentialDriveIO io;
    private final DifferentialDriveIO.DifferentialDriveInputs inputs = new DifferentialDriveIO.DifferentialDriveInputs();

    private double commandedVx = 0.0;
    private double commandedOmega = 0.0;

    // Ported WPILib kinematics (0.6 meters track width)
    private final org.areslib.math.kinematics.DifferentialDriveKinematics kinematics;

    private final org.areslib.math.controller.PIDController leftPid;
    private final org.areslib.math.controller.PIDController rightPid;
    private final org.areslib.math.controller.SimpleMotorFeedforward driveFeedforward;

    private final org.areslib.math.filter.SlewRateLimiter fwdLimiter;
    private final org.areslib.math.filter.SlewRateLimiter rotLimiter;

    /**
     * Constructs the DifferentialDriveSubsystem.
     * @param io The unified Differential Hardware interface.
     * @param config The structural parameters mapping physics to motor outputs.
     */
    public DifferentialDriveSubsystem(DifferentialDriveIO io, DifferentialConfig config) {
        this.io = io;
        this.kinematics = new org.areslib.math.kinematics.DifferentialDriveKinematics(config.trackwidthMeters);
        
        this.leftPid = new org.areslib.math.controller.PIDController(config.driveKp, config.driveKi, config.driveKd);
        this.rightPid = new org.areslib.math.controller.PIDController(config.driveKp, config.driveKi, config.driveKd);
        
        this.driveFeedforward = new org.areslib.math.controller.SimpleMotorFeedforward(config.driveKs, config.driveKv);

        if (config.maxAccelerationMps2 > 0.0) {
            this.fwdLimiter = new org.areslib.math.filter.SlewRateLimiter(config.maxAccelerationMps2);
            this.rotLimiter = new org.areslib.math.filter.SlewRateLimiter(config.maxAccelerationMps2 * 2.0); // Allow faster rotational accel
        } else {
            this.fwdLimiter = null;
            this.rotLimiter = null;
        }
    }

    @Override
    public void periodic() {
        io.updateInputs(inputs);
        org.areslib.telemetry.AresAutoLogger.processInputs("DifferentialDrive", inputs);
    }

    /**
     * @return The commanded X velocity in m/s.
     */
    @Override
    public double getCommandedVx() {
        return commandedVx;
    }

    @Override
    public double getCommandedVy() {
        return 0.0; // Differential drive cannot strafe
    }

    @Override
    public double getCommandedOmega() {
        return commandedOmega;
    }

    /**
     * Commands the differential drive to move in a field-centric manner.
     * Note: Differential drives cannot strafe, so the Y component is discarded.
     * The heading rotation is used to transform the forward command relative to the field.
     * @param vxMetersPerSec The X velocity (field relative) in m/s.
     * @param vyMetersPerSec The Y velocity (field relative) in m/s — ignored for differential.
     * @param omegaRadPerSec The angular velocity in rad/s.
     * @param robotHeading The current robot heading.
     */
    public void driveFieldCentric(double vxMetersPerSec, double vyMetersPerSec, double omegaRadPerSec, org.areslib.math.geometry.Rotation2d robotHeading) {
        org.areslib.math.kinematics.ChassisSpeeds speeds = org.areslib.math.kinematics.ChassisSpeeds.fromFieldRelativeSpeeds(
            vxMetersPerSec, vyMetersPerSec, omegaRadPerSec, robotHeading
        );
        // Differential drives can only use forward (vx) and turn (omega); strafe (vy) is dropped
        drive(speeds.vxMetersPerSecond, speeds.omegaRadiansPerSecond);
    }

    /**
     * Commands the differential drive to move.
     * @param forwardMetersPerSec The forward velocity in m/s (X axis).
     * @param turnRadPerSec The angular velocity in rad/s.
     */
    public void drive(double forwardMetersPerSec, double turnRadPerSec) {
        if (fwdLimiter != null) {
            forwardMetersPerSec = fwdLimiter.calculate(forwardMetersPerSec, 0.02);
            turnRadPerSec = rotLimiter.calculate(turnRadPerSec, 0.02);
        }

        this.commandedVx = forwardMetersPerSec;
        this.commandedOmega = turnRadPerSec;
        
        org.areslib.math.kinematics.ChassisSpeeds speeds = new org.areslib.math.kinematics.ChassisSpeeds(
            forwardMetersPerSec, 0.0, turnRadPerSec
        );
        speeds = org.areslib.math.kinematics.ChassisSpeeds.discretize(speeds, 0.02);

        org.areslib.math.kinematics.DifferentialDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(speeds);

        double leftVolts = driveFeedforward.calculate(wheelSpeeds.leftMetersPerSecond) 
            + leftPid.calculate(inputs.leftVelocityMps, wheelSpeeds.leftMetersPerSecond);
            
        double rightVolts = driveFeedforward.calculate(wheelSpeeds.rightMetersPerSecond) 
            + rightPid.calculate(inputs.rightVelocityMps, wheelSpeeds.rightMetersPerSecond);

        io.setVoltages(leftVolts, rightVolts);
    }
}

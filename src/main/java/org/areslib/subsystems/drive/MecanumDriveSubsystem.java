package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;

/**
 * AdvantageKit-style Mecanum Drive Subsystem.
 * Acts as the structural controller for handling physics logic across all four wheels.
 */
public class MecanumDriveSubsystem extends SubsystemBase {

    private final MecanumDriveIO io;
    private final MecanumDriveIO.MecanumDriveInputs inputs = new MecanumDriveIO.MecanumDriveInputs();

    private double commandedVx = 0.0;
    private double commandedVy = 0.0;
    private double commandedOmega = 0.0;

    // Ported WPILib kinematics
    private final org.areslib.math.kinematics.MecanumDriveKinematics kinematics = new org.areslib.math.kinematics.MecanumDriveKinematics(
            new org.areslib.math.geometry.Translation2d(0.3, 0.3),   // FL
            new org.areslib.math.geometry.Translation2d(0.3, -0.3),  // FR
            new org.areslib.math.geometry.Translation2d(-0.3, 0.3),  // RL
            new org.areslib.math.geometry.Translation2d(-0.3, -0.3)  // RR
    );

    private final org.areslib.math.controller.PIDController frontLeftPid = new org.areslib.math.controller.PIDController(1.0, 0.0, 0.0);
    private final org.areslib.math.controller.PIDController frontRightPid = new org.areslib.math.controller.PIDController(1.0, 0.0, 0.0);
    private final org.areslib.math.controller.PIDController rearLeftPid = new org.areslib.math.controller.PIDController(1.0, 0.0, 0.0);
    private final org.areslib.math.controller.PIDController rearRightPid = new org.areslib.math.controller.PIDController(1.0, 0.0, 0.0);
    private final org.areslib.math.controller.SimpleMotorFeedforward driveFeedforward = new org.areslib.math.controller.SimpleMotorFeedforward(0.1, 2.5);

    /**
     * Constructs the MecanumDriveSubsystem.
     * @param io The unified Mecanum Hardware interface.
     */
    public MecanumDriveSubsystem(MecanumDriveIO io) {
        this.io = io;
    }

    @Override
    public void periodic() {
        io.updateInputs(inputs);
        org.areslib.telemetry.AresAutoLogger.processInputs("MecanumDrive", inputs);
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
        this.commandedVx = forwardMetersPerSec;
        this.commandedVy = strafeMetersPerSec;
        this.commandedOmega = turnRadPerSec;
        
        org.areslib.math.kinematics.ChassisSpeeds speeds = new org.areslib.math.kinematics.ChassisSpeeds(
            forwardMetersPerSec, strafeMetersPerSec, turnRadPerSec
        );

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

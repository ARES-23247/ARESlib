package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;
import org.areslib.hardware.SwerveModuleIO;

public class DriveSubsystem extends SubsystemBase {

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
    private final org.areslib.math.kinematics.SwerveDriveKinematics kinematics = new org.areslib.math.kinematics.SwerveDriveKinematics(
            new org.areslib.math.geometry.Translation2d(0.3, 0.3),   // FL
            new org.areslib.math.geometry.Translation2d(0.3, -0.3),  // FR
            new org.areslib.math.geometry.Translation2d(-0.3, 0.3),  // BL
            new org.areslib.math.geometry.Translation2d(-0.3, -0.3)  // BR
    );

    private final org.areslib.math.controller.PIDController[] drivePids = new org.areslib.math.controller.PIDController[4];
    private final org.areslib.math.controller.PIDController[] turnPids = new org.areslib.math.controller.PIDController[4];
    private final org.areslib.math.controller.SimpleMotorFeedforward driveFeedforward = new org.areslib.math.controller.SimpleMotorFeedforward(0.1, 2.5);

    public DriveSubsystem(
            SwerveModuleIO frontLeft, 
            SwerveModuleIO frontRight, 
            SwerveModuleIO backLeft, 
            SwerveModuleIO backRight) {
        this.frontLeft = frontLeft;
        this.frontRight = frontRight;
        this.backLeft = backLeft;
        this.backRight = backRight;

        for (int i = 0; i < 4; i++) {
            drivePids[i] = new org.areslib.math.controller.PIDController(1.0, 0.0, 0.0);
            turnPids[i] = new org.areslib.math.controller.PIDController(3.0, 0.0, 0.0);
            turnPids[i].enableContinuousInput(-Math.PI, Math.PI);
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
    }

    public double getCommandedVx() { return commandedVx; }
    public double getCommandedVy() { return commandedVy; }
    public double getCommandedOmega() { return commandedOmega; }

    public void drive(double forwardMetersPerSec, double strafeMetersPerSec, double turnRadPerSec) {
        this.commandedVx = forwardMetersPerSec;
        this.commandedVy = strafeMetersPerSec;
        this.commandedOmega = turnRadPerSec;
        
        org.areslib.math.kinematics.ChassisSpeeds speeds = new org.areslib.math.kinematics.ChassisSpeeds(
            forwardMetersPerSec, strafeMetersPerSec, turnRadPerSec
        );
        org.areslib.math.kinematics.SwerveModuleState[] states = kinematics.toSwerveModuleStates(speeds);

        SwerveModuleIO[] modules = {frontLeft, frontRight, backLeft, backRight};
        SwerveModuleIO.SwerveModuleInputs[] inputs = {flInputs, frInputs, blInputs, brInputs};

        for (int i = 0; i < 4; i++) {
            double targetSpeedMps = states[i].speedMetersPerSecond;
            double targetAngleRad = states[i].angle.getRadians();

            double feedforwardVolts = driveFeedforward.calculate(targetSpeedMps);
            double drivePidOut = drivePids[i].calculate(inputs[i].driveVelocityMps, targetSpeedMps);
            
            double turnPidOut = turnPids[i].calculate(inputs[i].turnAbsolutePositionRad, targetAngleRad);

            modules[i].setDriveVoltage(feedforwardVolts + drivePidOut);
            modules[i].setTurnVoltage(turnPidOut);
        }
    }
}

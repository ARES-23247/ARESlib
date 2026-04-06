package org.areslib.subsystems.drive;

import org.areslib.command.SubsystemBase;

/**
 * AdvantageKit-style Swerve Drive Subsystem.
 * Acts as the structural controller for handling physics logic across four modules.
 */
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

    /**
     * Constructs the DriveSubsystem.
     * @param frontLeft The front left module IO.
     * @param frontRight The front right module IO.
     * @param backLeft The back left module IO.
     * @param backRight The back right module IO.
     */
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
     * Commands the swerve drive to move.
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
        org.areslib.math.kinematics.SwerveModuleState[] states = kinematics.toSwerveModuleStates(speeds);

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

            modules[i].setDriveVoltage(feedforwardVolts + drivePidOut);
            modules[i].setTurnVoltage(turnPidOut);
        }
    }
}

package org.firstinspires.ftc.teamcode.subsystems.drive;

import org.areslib.hardware.devices.AresMotorModel;

public class DifferentialConfig {

    // --- PID and Feedforward ---
    public double driveKp = 1.0;
    public double driveKi = 0.0;
    public double driveKd = 0.0;

    public double driveKs = 0.1;
    public double driveKv = 2.5;

    // --- Physics Definitions ---
    /** Trackwidth (distance between left and right wheels) in meters */
    public double trackwidthMeters = 0.6; // ~24 inches

    // --- Hardware Conversions ---
    /** Diameter of the drive wheel in millimeters. */
    public double wheelDiameterMM = 104.0; // standard 104mm goBILDA wheel
    
    /** The predefined motor/encoder model attached to the wheel. */
    public AresMotorModel driveMotorModel = AresMotorModel.GOBILDA_5203_312_RPM;
    
    /** Custom ticks per revolution, only used if driveMotorModel is CUSTOM. */
    public double customDriveTicksPerRev = 8192.0;

    /** External gear reduction from the encoder output to the wheel (e.g. 2.0 if the wheel spins twice as slow). */
    public double driveExternalGearRatio = 1.0;
    
    // --- Control Parameters ---
    /** The maximum linear acceleration of the robot in meters per second squared. 0.0 disables slew rate limiters. */
    public double maxAccelerationMps2 = 0.0;
    
    /** 
     * Calculates the physical distance traveled per encoder tick.
     * @return meters per encoder tick.
     */
    public double getDriveMetersPerTick() {
        double wheelRadiusMeters = (wheelDiameterMM / 2.0) / 1000.0;
        double ticksPerRev = (driveMotorModel == AresMotorModel.CUSTOM) ?
                             customDriveTicksPerRev : driveMotorModel.getTicksPerRev();
        return (2 * Math.PI * wheelRadiusMeters) / (ticksPerRev * driveExternalGearRatio);
    }
}

package org.areslib.subsystems.drive;

/**
 * Configuration data class for the SwerveDriveSubsystem.
 * Contains physics and tuning parameters specific to a robot's physical construction.
 */
public class SwerveConfig {
    
    // --- Kinematics (Wheelbase) ---
    /** The distance from the center of the robot to the wheels along the X axis (meters). */
    public double trackWidthXMeters = 0.3; 
    /** The distance from the center of the robot to the wheels along the Y axis (meters). */
    public double trackWidthYMeters = 0.3;

    // --- Tuning (Drive) ---
    /** Proportional gain for the drive velocity PID controller. */
    public double driveKp = 1.0;
    /** Integral gain for the drive velocity PID controller. */
    public double driveKi = 0.0;
    /** Derivative gain for the drive velocity PID controller. */
    public double driveKd = 0.0;

    /** Static Feedforward gain for the drive motors (overcoming friction). */
    public double driveKs = 0.1;
    /** Velocity Feedforward gain for the drive motors. */
    public double driveKv = 2.5;

    // --- Tuning (Turn) ---
    /** Proportional gain for the turning position PID controller. */
    public double turnKp = 3.0;
    /** Integral gain for the turning position PID controller. */
    public double turnKi = 0.0;
    /** Derivative gain for the turning position PID controller. */
    public double turnKd = 0.0;
    /** Static Feedforward gain for the turn motors. */
    public double turnKs = 0.2;

    // --- Hardware Conversions ---
    /** Diameter of the drive wheel in millimeters. */
    public double wheelDiameterMM = 104.0;
    
    /** The predefined motor/encoder model attached to the wheel. */
    public org.areslib.hardware.devices.AresMotorModel driveMotorModel = org.areslib.hardware.devices.AresMotorModel.REV_THROUGH_BORE;
    
    /** Custom ticks per revolution, only used if driveMotorModel is CUSTOM. */
    public double customDriveTicksPerRev = 8192.0;
    
    /** External gear reduction from the encoder output to the wheel (e.g. 2.0 if the wheel spins twice as slow due to a belt/chain). */
    public double driveExternalGearRatio = 1.0;
    
    /** 
     * Calculates the physical distance traveled per encoder tick using the wheel diameter, motor intrinsic resolution, and external gear ratio.
     * @return meters per encoder tick.
     */
    public double getDriveMetersPerTick() {
        double wheelRadiusMeters = (wheelDiameterMM / 2.0) / 1000.0;
        double ticksPerRev = (driveMotorModel == org.areslib.hardware.devices.AresMotorModel.CUSTOM) ?
                             customDriveTicksPerRev : driveMotorModel.getTicksPerRev();
        return (2 * Math.PI * wheelRadiusMeters) / (ticksPerRev * driveExternalGearRatio);
    }
    
    // --- Control Parameters ---
    /** The maximum linear acceleration of the robot in meters per second squared. 0.0 disables slew rate limiters. */
    public double maxAccelerationMps2 = 0.0;

    /** Maximum safe module speed in meters per second. */
    public double maxModuleSpeedMps = 4.0;
}

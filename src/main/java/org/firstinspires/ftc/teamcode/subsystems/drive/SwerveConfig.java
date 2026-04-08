package org.firstinspires.ftc.teamcode.subsystems.drive;

import org.areslib.subsystems.drive.SwerveDriveSubsystem;

/**
 * Team-specific swerve configuration that extends the library's base config.
 *
 * <p>This class adds hardware-specific parameters (wheel diameter, encoder model, gear ratios) on
 * top of the control tuning parameters inherited from {@link SwerveDriveSubsystem.Config}.
 */
public class SwerveConfig extends SwerveDriveSubsystem.Config {

  // --- Hardware Conversions ---
  /** Diameter of the drive wheel in millimeters. */
  public double wheelDiameterMM = 104.0;

  /** The predefined motor/encoder model attached to the wheel. */
  public org.areslib.hardware.devices.AresMotorModel driveMotorModel =
      org.areslib.hardware.devices.AresMotorModel.REV_THROUGH_BORE;

  /** Custom ticks per revolution, only used if driveMotorModel is CUSTOM. */
  public double customDriveTicksPerRev = 8192.0;

  /**
   * External gear reduction from the encoder output to the wheel (e.g. 2.0 if the wheel spins twice
   * as slow due to a belt/chain).
   */
  public double driveExternalGearRatio = 1.0;

  /**
   * Calculates the physical distance traveled per encoder tick using the wheel diameter, motor
   * intrinsic resolution, and external gear ratio.
   *
   * @return meters per encoder tick.
   */
  public double getDriveMetersPerTick() {
    double wheelRadiusMeters = (wheelDiameterMM / 2.0) / 1000.0;
    double ticksPerRev =
        (driveMotorModel == org.areslib.hardware.devices.AresMotorModel.CUSTOM)
            ? customDriveTicksPerRev
            : driveMotorModel.getTicksPerRev();
    return (2 * Math.PI * wheelRadiusMeters) / (ticksPerRev * driveExternalGearRatio);
  }
}

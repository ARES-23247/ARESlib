package org.firstinspires.ftc.teamcode.subsystems.drive;

import org.areslib.hardware.devices.AresMotorModel;
import org.areslib.subsystems.drive.DifferentialDriveSubsystem;

/**
 * Team-specific differential configuration that extends the library's base config.
 *
 * <p>This class adds hardware-specific parameters (wheel diameter, encoder model, gear ratios) on
 * top of the tuning and physics parameters inherited from {@link
 * DifferentialDriveSubsystem.Config}.
 */
public class DifferentialConfig extends DifferentialDriveSubsystem.Config {

  // --- Hardware Conversions ---
  /** Diameter of the drive wheel in millimeters. */
  public double wheelDiameterMM = 104.0; // standard 104mm goBILDA wheel

  /** The predefined motor/encoder model attached to the wheel. */
  public AresMotorModel driveMotorModel = AresMotorModel.GOBILDA_5203_312_RPM;

  /** Custom ticks per revolution, only used if driveMotorModel is CUSTOM. */
  public double customDriveTicksPerRev = 8192.0;

  /**
   * External gear reduction from the encoder output to the wheel (e.g. 2.0 if the wheel spins twice
   * as slow).
   */
  public double driveExternalGearRatio = 1.0;

  /**
   * Calculates the physical distance traveled per encoder tick.
   *
   * @return meters per encoder tick.
   */
  public double getDriveMetersPerTick() {
    double wheelRadiusMeters = (wheelDiameterMM / 2.0) / 1000.0;
    double ticksPerRev =
        (driveMotorModel == AresMotorModel.CUSTOM)
            ? customDriveTicksPerRev
            : driveMotorModel.getTicksPerRev();
    return (2 * Math.PI * wheelRadiusMeters) / (ticksPerRev * driveExternalGearRatio);
  }
}

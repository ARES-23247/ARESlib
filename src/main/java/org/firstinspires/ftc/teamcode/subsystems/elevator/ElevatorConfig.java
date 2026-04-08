package org.firstinspires.ftc.teamcode.subsystems.elevator;

import org.areslib.hardware.devices.AresMotorModel;

public class ElevatorConfig {

  // --- Physics Definitions ---
  /** Diameter of the spool or pinion gear driving the elevator in millimeters. */
  public double spoolDiameterMM = 38.0; // e.g., 38mm spool

  /** The predefined motor/encoder model attached to the elevator. */
  public AresMotorModel motorModel = AresMotorModel.GOBILDA_5203_312_RPM;

  /** Custom ticks per revolution, only used if motorModel is CUSTOM. */
  public double customTicksPerRev = 8192.0;

  /**
   * External gear reduction from the encoder output to the spool (e.g. 2.0 if the spool spins twice
   * as slow).
   */
  public double externalGearRatio = 1.0;

  /**
   * Calculates the physical linear distance traveled per encoder tick.
   *
   * @return meters per encoder tick.
   */
  public double getMetersPerTick() {
    double spoolRadiusMeters = (spoolDiameterMM / 2.0) / 1000.0;
    double ticksPerRev =
        (motorModel == AresMotorModel.CUSTOM) ? customTicksPerRev : motorModel.getTicksPerRev();
    return (2 * Math.PI * spoolRadiusMeters) / (ticksPerRev * externalGearRatio);
  }
}

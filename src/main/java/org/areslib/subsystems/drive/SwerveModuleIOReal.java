package org.areslib.subsystems.drive;

import org.areslib.hardware.interfaces.AresAbsoluteEncoder;
import org.areslib.hardware.interfaces.AresEncoder;
import org.areslib.hardware.interfaces.AresMotor;

/**
 * Concrete implementation of {@link SwerveModuleIO} for physical robot hardware.
 *
 * <p>This class maps the logical swerve module commands to actual FTC motors and encoders.
 */
public class SwerveModuleIOReal implements SwerveModuleIO {

  private final AresMotor driveMotor;
  private final AresMotor turnMotor;
  private final AresEncoder driveEncoder;
  private final AresAbsoluteEncoder turnEncoder;
  private final double driveMetersPerTick;

  /**
   * Constructs a physical Swerve Module IO layer.
   *
   * @param driveMotor The hardware wrapper for the driving motor.
   * @param turnMotor The hardware wrapper for the steering/turning motor.
   * @param driveEncoder The hardware wrapper for the driving encoder.
   * @param turnEncoder The hardware wrapper for the absolute steering encoder.
   * @param driveMetersPerTick Meters traveled per encoder tick for the drive motor.
   */
  public SwerveModuleIOReal(
      AresMotor driveMotor,
      AresMotor turnMotor,
      AresEncoder driveEncoder,
      AresAbsoluteEncoder turnEncoder,
      double driveMetersPerTick) {
    this.driveMotor = driveMotor;
    this.turnMotor = turnMotor;
    this.driveEncoder = driveEncoder;
    this.turnEncoder = turnEncoder;
    this.driveMetersPerTick = driveMetersPerTick;
  }

  @Override
  public void updateInputs(SwerveModuleInputs inputs) {
    // Direct, zero-latency reads from the abstracted cache.
    // Convert to physical units immediately.
    inputs.drivePositionMeters = driveEncoder.getPosition() * driveMetersPerTick;
    inputs.driveVelocityMps = driveEncoder.getVelocity() * driveMetersPerTick;

    inputs.turnAbsolutePositionRad = turnEncoder.getAbsolutePositionRad();
    inputs.turnVelocityRadPerSec =
        turnEncoder
            .getVelocity(); // Assuming it returns natively what it's set to, or that turn is just
    // position controlled
  }

  @Override
  public void setDriveVoltage(double volts) {
    driveMotor.setVoltage(volts);
  }

  @Override
  public void setTurnVoltage(double volts) {
    turnMotor.setVoltage(volts);
  }
}

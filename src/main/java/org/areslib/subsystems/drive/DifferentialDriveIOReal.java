package org.areslib.subsystems.drive;

import org.areslib.hardware.interfaces.AresEncoder;
import org.areslib.hardware.interfaces.AresMotor;

/**
 * Concrete implementation of {@link DifferentialDriveIO} for physical robot hardware.
 *
 * <p>This class maps the logical tank drive commands to actual FTC motors and encoders.
 */
public class DifferentialDriveIOReal implements DifferentialDriveIO {

  private final AresMotor leftMotor;
  private final AresMotor rightMotor;

  private final AresEncoder leftEncoder;
  private final AresEncoder rightEncoder;

  private final double distancePerTick;

  /**
   * Constructs a physical DifferentialDrive IO layer.
   *
   * @param leftMotor The hardware wrapper for the left driveline motor(s).
   * @param rightMotor The hardware wrapper for the right driveline motor(s).
   * @param leftEncoder The hardware wrapper for the left driveline encoder.
   * @param rightEncoder The hardware wrapper for the right driveline encoder.
   * @param distancePerTick A scalar converting raw encoder ticks into meters of linear travel.
   */
  public DifferentialDriveIOReal(
      AresMotor leftMotor,
      AresMotor rightMotor,
      AresEncoder leftEncoder,
      AresEncoder rightEncoder,
      double distancePerTick) {
    this.leftMotor = leftMotor;
    this.rightMotor = rightMotor;
    this.leftEncoder = leftEncoder;
    this.rightEncoder = rightEncoder;
    this.distancePerTick = distancePerTick;
  }

  @Override
  public void updateInputs(DifferentialDriveInputs inputs) {
    inputs.leftPositionMeters = leftEncoder.getPosition() * distancePerTick;
    inputs.leftVelocityMps = leftEncoder.getVelocity() * distancePerTick;

    inputs.rightPositionMeters = rightEncoder.getPosition() * distancePerTick;
    inputs.rightVelocityMps = rightEncoder.getVelocity() * distancePerTick;
  }

  @Override
  public void setVoltages(double leftVolts, double rightVolts) {
    leftMotor.setVoltage(leftVolts);
    rightMotor.setVoltage(rightVolts);
  }
}

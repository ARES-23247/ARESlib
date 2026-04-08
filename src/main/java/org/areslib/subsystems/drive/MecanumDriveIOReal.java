package org.areslib.subsystems.drive;

import org.areslib.hardware.interfaces.AresEncoder;
import org.areslib.hardware.interfaces.AresMotor;

/**
 * Concrete implementation of {@link MecanumDriveIO} for physical robot hardware.
 *
 * <p>This class maps the logical mecanum drive commands to actual FTC motors and encoders across
 * the four drivetrains.
 */
public class MecanumDriveIOReal implements MecanumDriveIO {

  private final AresMotor flMotor, frMotor, rlMotor, rrMotor;
  private final AresEncoder flEncoder, frEncoder, rlEncoder, rrEncoder;
  private final double distancePerTick;

  /**
   * Constructs a physical MecanumDrive IO layer.
   *
   * @param flMotor The hardware wrapper for the front-left motor.
   * @param frMotor The hardware wrapper for the front-right motor.
   * @param rlMotor The hardware wrapper for the rear-left motor.
   * @param rrMotor The hardware wrapper for the rear-right motor.
   * @param flEncoder The hardware wrapper for the front-left encoder.
   * @param frEncoder The hardware wrapper for the front-right encoder.
   * @param rlEncoder The hardware wrapper for the rear-left encoder.
   * @param rrEncoder The hardware wrapper for the rear-right encoder.
   * @param distancePerTick A scalar converting raw encoder ticks into meters of linear travel.
   */
  public MecanumDriveIOReal(
      AresMotor flMotor,
      AresMotor frMotor,
      AresMotor rlMotor,
      AresMotor rrMotor,
      AresEncoder flEncoder,
      AresEncoder frEncoder,
      AresEncoder rlEncoder,
      AresEncoder rrEncoder,
      double distancePerTick) {
    this.flMotor = flMotor;
    this.frMotor = frMotor;
    this.rlMotor = rlMotor;
    this.rrMotor = rrMotor;
    this.flEncoder = flEncoder;
    this.frEncoder = frEncoder;
    this.rlEncoder = rlEncoder;
    this.rrEncoder = rrEncoder;
    this.distancePerTick = distancePerTick;
  }

  @Override
  public void updateInputs(MecanumDriveInputs inputs) {
    inputs.frontLeftPositionMeters = flEncoder.getPosition() * distancePerTick;
    inputs.frontLeftVelocityMps = flEncoder.getVelocity() * distancePerTick;

    inputs.frontRightPositionMeters = frEncoder.getPosition() * distancePerTick;
    inputs.frontRightVelocityMps = frEncoder.getVelocity() * distancePerTick;

    inputs.rearLeftPositionMeters = rlEncoder.getPosition() * distancePerTick;
    inputs.rearLeftVelocityMps = rlEncoder.getVelocity() * distancePerTick;

    inputs.rearRightPositionMeters = rrEncoder.getPosition() * distancePerTick;
    inputs.rearRightVelocityMps = rrEncoder.getVelocity() * distancePerTick;
  }

  @Override
  public void setVoltages(double frontLeft, double frontRight, double rearLeft, double rearRight) {
    flMotor.setVoltage(frontLeft);
    frMotor.setVoltage(frontRight);
    rlMotor.setVoltage(rearLeft);
    rrMotor.setVoltage(rearRight);
  }
}

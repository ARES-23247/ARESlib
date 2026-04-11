package org.firstinspires.ftc.teamcode.subsystems.elevator;

import org.areslib.hardware.wrappers.DcMotorExWrapper;

/**
 * ElevatorIOReal standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * ElevatorIOReal}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public class ElevatorIOReal implements ElevatorIO {
  private final DcMotorExWrapper motor;
  private final double distancePerTick;
  private double appliedVolts = 0.0;

  public ElevatorIOReal(DcMotorExWrapper motor, double distancePerTick) {
    this.motor = motor;
    this.distancePerTick = distancePerTick;
    this.motor.setCurrentPolling(true);
  }

  @Override
  public void updateInputs(ElevatorIOInputs inputs) {
    inputs.positionMeters = motor.getPosition() * distancePerTick;
    inputs.velocityMetersPerSec = motor.getVelocity() * distancePerTick;
    inputs.appliedVolts = appliedVolts;
    inputs.currentAmps[0] = motor.getCurrentAmps();
  }

  @Override
  public void setVoltage(double volts) {
    appliedVolts = volts;
    motor.setVoltage(volts);
  }
}

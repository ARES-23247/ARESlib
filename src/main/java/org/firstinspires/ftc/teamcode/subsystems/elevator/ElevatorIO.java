package org.firstinspires.ftc.teamcode.subsystems.elevator;

import org.areslib.telemetry.AresLoggableInputs;

/**
 * ElevatorIO standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * ElevatorIO}. Extracted and compiled as part of the ARESLib2 Code Audit for missing documentation
 * coverage.
 */
public interface ElevatorIO {
  public static class ElevatorIOInputs implements AresLoggableInputs {
    public double positionMeters = 0.0;
    public double velocityMetersPerSec = 0.0;
    public double appliedVolts = 0.0;
    public double[] currentAmps = new double[] {};
  }

  public default void updateInputs(ElevatorIOInputs inputs) {}

  public default void setVoltage(double volts) {}
}

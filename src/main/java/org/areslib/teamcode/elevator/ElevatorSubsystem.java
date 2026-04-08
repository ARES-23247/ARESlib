package org.areslib.teamcode.elevator;

import org.areslib.command.SubsystemBase;
import org.areslib.telemetry.AresAutoLogger;

/**
 * AdvantageKit-style Elevator Subsystem. Uses an IO interface to separate logic from physical
 * hardware, allowing for simulation.
 */
public class ElevatorSubsystem extends SubsystemBase {

  public final ElevatorIO io;
  private final ElevatorIO.ElevatorInputs inputs = new ElevatorIO.ElevatorInputs();

  /**
   * Constructs a new ElevatorSubsystem with the specified IO interface.
   *
   * @param io The IO interface (real or sim) used by the subsystem.
   */
  public ElevatorSubsystem(ElevatorIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    // Update inputs every loop from cache or sim
    io.updateInputs(inputs);

    // Magically pushes all structure data to both Live WebSocket and .wpilog simultaneously.
    AresAutoLogger.processInputs("Elevator", inputs);

    // Note: Because of this architecture, adding a limit switch requires exactly two steps:
    // 1. Adding a boolean to ElevatorInputs.
    // 2. Reading it in ElevatorIOReal.
    // The AutoLogger dynamically handles ALL parsing, UI generation, and binary logging!
  }

  /**
   * Sets the voltage applied to the elevator motor.
   *
   * @param volts The target voltage (-12.0 to 12.0).
   */
  public void setVoltage(double volts) {
    io.setVoltage(volts);
  }

  /** Closes the grabber servo. */
  public void closeGrabber() {
    io.setGrabberServo(1.0);
  }

  /** Opens the grabber servo. */
  public void openGrabber() {
    io.setGrabberServo(0.0);
  }

  /**
   * Gets the current position of the elevator in meters.
   *
   * @return The position in meters.
   */
  public double getPositionMeters() {
    return inputs.positionMeters;
  }

  /**
   * Gets the current velocity of the elevator in meters per second.
   *
   * @return The velocity in meters per second.
   */
  public double getVelocityMps() {
    return inputs.velocityMps;
  }

  /**
   * Gets the current inputs structured data.
   *
   * @return The inputs object.
   */
  public ElevatorIO.ElevatorInputs getInputs() {
    return inputs;
  }

  /** Resets the elevator encoder to zero. */
  public void resetEncoder() {
    io.resetEncoder();
  }
}

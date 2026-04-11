package org.firstinspires.ftc.teamcode.subsystems.elevator;

import static org.firstinspires.ftc.teamcode.Constants.ElevatorConstants.*;

import org.areslib.command.SubsystemBase;
import org.areslib.telemetry.AresAutoLogger;

/**
 * ElevatorSubsystem standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * ElevatorSubsystem}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public class ElevatorSubsystem extends SubsystemBase {

  private final ElevatorIO io;
  private final ElevatorIO.ElevatorIOInputs inputs = new ElevatorIO.ElevatorIOInputs();

  // Standard PID gains for demonstration (teams should tune these)
  private double targetPositionMeters = 0.0;

  public ElevatorSubsystem(ElevatorIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    AresAutoLogger.processInputs("Elevator", inputs);

    // Compute error
    double error = targetPositionMeters - inputs.positionMeters;

    // Proportional Control with constant gravity feedforward (kG).
    // DESIGN NOTE: kG is always applied upward regardless of direction. This creates an
    // intentionally asymmetric response: the elevator moves up at full P-speed but descends
    // more slowly because kG opposes the downward P-output. This is a safety feature —
    // it prevents free-fall if the P term underestimates gravity or the motor loses power.
    // Teams tuning for faster descent should increase kP, NOT remove kG.
    double volts = (error * P) + G;

    if (inputs.positionMeters >= MAX_POSITION_METERS && volts > G) {
      // At upper limit: only apply gravity hold, don't push higher
      volts = G;
    } else if (inputs.positionMeters <= MIN_POSITION_METERS && error <= 0.0) {
      // At floor with no upward demand: zero output to prevent grinding into hard stop
      volts = 0.0;
    }

    io.setVoltage(volts);

    AresAutoLogger.recordOutput("Elevator/TargetPositionMeters", targetPositionMeters);
    AresAutoLogger.recordOutput("Elevator/ErrorMeters", error);
  }

  public void setTargetPosition(double positionMeters) {
    this.targetPositionMeters =
        Math.max(MIN_POSITION_METERS, Math.min(positionMeters, MAX_POSITION_METERS)); // Clamp
  }

  public double getPositionMeters() {
    return inputs.positionMeters;
  }

  public double getVelocityMetersPerSec() {
    return inputs.velocityMetersPerSec;
  }
}

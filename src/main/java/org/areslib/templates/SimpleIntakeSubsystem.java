package org.areslib.templates;

import org.areslib.command.SubsystemBase;
import org.areslib.telemetry.AresTelemetry;

/**
 * STARTER TEMPLATE: Simple intake subsystem with one motor and one sensor.
 *
 * <p>Copy this file into your teamcode package and rename it. This demonstrates the core ARESLib
 * patterns:
 *
 * <ul>
 *   <li>IO abstraction (swap between real hardware and simulation)
 *   <li>Periodic telemetry logging
 *   <li>State machine for intake control
 * </ul>
 *
 * <h3>Quick Start:</h3>
 *
 * <pre>
 * // In your OpMode:
 * IntakeIO io = new IntakeIOReal(hardwareMap);    // On real robot
 * IntakeIO io = new IntakeIOSim();                // In simulation
 * IntakeSubsystem intake = new IntakeSubsystem(io);
 * </pre>
 */
public class SimpleIntakeSubsystem extends SubsystemBase {

  // ========== IO Interface ==========
  // This is the hardware abstraction. ALL hardware access goes through here.
  public interface IntakeIO {
    /** Container for sensor readings. Updated every 20ms loop. */
    class IntakeInputs {
      public double motorCurrentAmps = 0.0;
      public boolean pieceDetected = false;
    }

    /** Read all sensors into the inputs object. */
    void updateInputs(IntakeInputs inputs);

    /** Command the intake motor voltage. */
    void setMotorVoltage(double volts);
  }

  // ========== IO Implementation: Real Hardware ==========
  // Uncomment and fill in for your actual robot hardware:
  /*
  public static class IntakeIOReal implements IntakeIO {
      private final DcMotorEx motor;
      private final DigitalChannel sensor;

      public IntakeIOReal(HardwareMap hwMap) {
          motor = hwMap.get(DcMotorEx.class, "intakeMotor");
          sensor = hwMap.get(DigitalChannel.class, "intakeSensor");
          sensor.setMode(DigitalChannel.Mode.INPUT);
      }

      @Override
      public void updateInputs(IntakeInputs inputs) {
          inputs.motorCurrentAmps = motor.getCurrent(CurrentUnit.AMPS);
          inputs.pieceDetected = !sensor.getState(); // Active low
      }

      @Override
      public void setMotorVoltage(double volts) {
          motor.setPower(volts / 12.0);
      }
  }
  */

  // ========== IO Implementation: Simulation ==========
  public static class IntakeIOSim implements IntakeIO {
    private double commandedVolts = 0.0;
    private boolean simPieceDetected = false;

    @Override
    public void updateInputs(IntakeInputs inputs) {
      inputs.motorCurrentAmps = Math.abs(commandedVolts) * 2.0; // Rough I estimate
      inputs.pieceDetected = simPieceDetected;
    }

    @Override
    public void setMotorVoltage(double volts) {
      this.commandedVolts = volts;
    }

    /** For testing: simulate a game piece entering the intake. */
    public void setSimPieceDetected(boolean detected) {
      this.simPieceDetected = detected;
    }
  }

  // ========== Subsystem Logic ==========

  public enum IntakeState {
    STOPPED,
    INTAKING,
    EJECTING
  }

  private final IntakeIO io;
  private final IntakeIO.IntakeInputs inputs = new IntakeIO.IntakeInputs();
  private IntakeState state = IntakeState.STOPPED;

  public SimpleIntakeSubsystem(IntakeIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    // Step 1: Read sensors
    io.updateInputs(inputs);

    // Step 2: Act on state
    switch (state) {
      case INTAKING:
        if (inputs.pieceDetected) {
          // Auto-stop when piece is grabbed
          state = IntakeState.STOPPED;
          io.setMotorVoltage(0.0);
        } else {
          io.setMotorVoltage(8.0); // 8V intake
        }
        break;
      case EJECTING:
        io.setMotorVoltage(-6.0); // Reverse
        break;
      case STOPPED:
      default:
        io.setMotorVoltage(0.0);
        break;
    }

    // Step 3: Log telemetry
    AresTelemetry.putString("Intake/State", state.name());
    AresTelemetry.putNumber("Intake/CurrentAmps", inputs.motorCurrentAmps);
    AresTelemetry.putBoolean("Intake/PieceDetected", inputs.pieceDetected);
  }

  // ========== Public API ==========

  public void startIntake() {
    state = IntakeState.INTAKING;
  }

  public void eject() {
    state = IntakeState.EJECTING;
  }

  public void stop() {
    state = IntakeState.STOPPED;
  }

  public boolean hasPiece() {
    return inputs.pieceDetected;
  }
}

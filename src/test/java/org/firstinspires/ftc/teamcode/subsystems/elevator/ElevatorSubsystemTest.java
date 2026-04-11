package org.firstinspires.ftc.teamcode.subsystems.elevator;

import static org.firstinspires.ftc.teamcode.Constants.ElevatorConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElevatorSubsystemTest {

  private static class MockElevatorIO implements ElevatorIO {
    public double position = 0.0;
    public double voltage = 0.0;

    @Override
    public void updateInputs(ElevatorIOInputs inputs) {
      inputs.positionMeters = position;
      inputs.velocityMetersPerSec = 0.0;
    }

    @Override
    public void setVoltage(double volts) {
      this.voltage = volts;
    }
  }

  private MockElevatorIO mockIO;
  private ElevatorSubsystem elevator;

  @BeforeEach
  void setup() {
    mockIO = new MockElevatorIO();
    elevator = new ElevatorSubsystem(mockIO);
  }

  @Test
  void testNormalOperationWithinLimits() {
    // Test pushing up within legal bounds
    mockIO.position = MIN_POSITION_METERS + 0.1;
    elevator.setTargetPosition(MAX_POSITION_METERS - 0.1);
    elevator.periodic();

    // Output should be positive (error * kP + kG)
    assertTrue(mockIO.voltage > G, "Elevator should output voltage > G to rise");
  }

  @Test
  void testSoftLimitsPreventOverextension() {
    // Simulating the elevator is physically at the max limit
    mockIO.position = MAX_POSITION_METERS;

    // Asking the elevator to go WAY beyond the max limit
    elevator.setTargetPosition(MAX_POSITION_METERS + 5.0);
    elevator.periodic();

    // Safety clamps should restrict TargetPosition, and output should be floored to just gravity
    // feedforward
    assertEquals(
        G,
        mockIO.voltage,
        0.001,
        "Voltage should clamp exactly at gravity feedforward at max height");
  }

  @Test
  void testSoftLimitsPreventCrashingIntoFloor() {
    // Simulating the elevator is physically at the floor
    mockIO.position = MIN_POSITION_METERS;

    // Asking the elevator to go below the floor
    elevator.setTargetPosition(MIN_POSITION_METERS - 5.0);
    elevator.periodic();

    // Subsystem logic should Zero the output to prevent grinding the motor
    assertEquals(
        0.0, mockIO.voltage, 0.001, "Voltage should clamp to exactly 0 to prevent floor crashing");
  }
}

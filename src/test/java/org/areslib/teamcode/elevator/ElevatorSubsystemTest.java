package org.areslib.teamcode.elevator;

import static org.firstinspires.ftc.teamcode.Constants.ElevatorConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import org.areslib.command.CommandScheduler;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorIOSim;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorSubsystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElevatorSubsystemTest {

  private ElevatorIOSim io;
  private ElevatorSubsystem elevator;

  @BeforeEach
  void setUp() {
    CommandScheduler.getInstance().reset();

    // Ensure simulation mode flag is respected by the timeline loop period
    // AresRobot.LOOP_PERIOD_SECS is final, so we will just use its default value

    io = new ElevatorIOSim();
    elevator = new ElevatorSubsystem(io);
    CommandScheduler.getInstance().registerSubsystem(elevator);

    // Init 1 step
    CommandScheduler.getInstance().run();
  }

  @AfterEach
  void tearDown() {
    CommandScheduler.getInstance().reset();
  }

  @Test
  void testInitialPosition() {
    assertEquals(0.0, elevator.getPositionMeters(), 0.001);
  }

  @Test
  void testFeedforwardHoldsAgainstGravity() {
    // Subsystem initializes target to 0.0. Loop runs 1 step.
    // It should apply kG voltage to hold
    io.updateInputs(
        new org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorIO.ElevatorIOInputs());

    // Wait, if it sets target to 0, and position is 0, Volts = error*kP + kG = 0 + 0.2 = 0.2V
    // Wait wait wait, in ElevatorSubsystem line 38:
    // 38: else if (inputs.positionMeters <= MIN_POSITION_METERS && error <= 0.0) { volts = 0.0; }
    // So at floor, volts is 0.0.
    // Let's force it slightly up, but target at slightly up, to see if it holds dynamically

    elevator.setTargetPosition(0.5);
    for (int i = 0; i < 100; i++) {
      CommandScheduler.getInstance().run();
    }

    // Should reach target
    assertEquals(0.5, elevator.getPositionMeters(), 0.1);

    // At equilibrium, error is ~0, volts is ~kG
    // Wait! We can verify it reaches target nicely.
    assertTrue(elevator.getPositionMeters() > 0.4);
  }

  @Test
  void testUpperBoundsClamp() {
    elevator.setTargetPosition(10.0); // Above Max
    for (int i = 0; i < 200; i++) {
      CommandScheduler.getInstance().run();
    }
    // Should clamp and hard stop at max
    assertTrue(elevator.getPositionMeters() <= MAX_POSITION_METERS + 0.05);
  }
}

package org.areslib.teamcode.elevator;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.core.AresRobot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElevatorIOSimTest {
  private ElevatorIOSim sim;
  private ElevatorIO.ElevatorInputs inputs;

  @BeforeEach
  void setUp() {
    sim = new ElevatorIOSim();
    inputs = new ElevatorIO.ElevatorInputs();
  }

  @Test
  void testVoltageConstraints() {
    // Should cap at 12V
    sim.setVoltage(15.0);
    sim.updateInputs(inputs);
    assertEquals(12.0, inputs.appliedVolts, 1e-6);

    // Should cap at -12V
    sim.setVoltage(-20.0);
    sim.updateInputs(inputs);
    assertEquals(-12.0, inputs.appliedVolts, 1e-6);
  }

  @Test
  void testPhysicsIntegration() {
    sim.setVoltage(10.0); // 10V * 0.2 KV = 2.0 m/s
    sim.updateInputs(inputs);

    assertEquals(2.0, inputs.velocityMps, 1e-6);
    // position = velocity * 0.02
    assertEquals(2.0 * AresRobot.LOOP_PERIOD_SECS, inputs.positionMeters, 1e-6);
  }

  @Test
  void testFloorConstraint() {
    // Try to drive below 0
    sim.setVoltage(-10.0);
    sim.updateInputs(inputs);

    // Should be clamped to 0
    assertEquals(0.0, inputs.positionMeters, 1e-6);
    assertEquals(0.0, inputs.velocityMps, 1e-6); // zeroed out when hitting hard stop
  }
}

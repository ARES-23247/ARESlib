package org.areslib.subsystems.drive;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DifferentialDriveSubsystemTest {

  private static class MockDifferentialIO implements DifferentialDriveIO {
    public double leftVolts = 0.0;
    public double rightVolts = 0.0;

    @Override
    public void updateInputs(DifferentialDriveInputs inputs) {
      inputs.leftVelocityMps = 0.0;
      inputs.rightVelocityMps = 0.0;
    }

    @Override
    public void setVoltages(double left, double right) {
      this.leftVolts = left;
      this.rightVolts = right;
    }
  }

  private MockDifferentialIO mockIO;
  private DifferentialDriveSubsystem subsystem;

  @BeforeEach
  void setup() {
    mockIO = new MockDifferentialIO();
    subsystem = new DifferentialDriveSubsystem(mockIO, new DifferentialDriveSubsystem.Config());
  }

  @Test
  void testForwardDriveCommandRoutesPositiveVoltage() {
    // 2 m/s forward, 0 turn
    subsystem.drive(2.0, 0.0);
    subsystem.periodic();

    assertTrue(mockIO.leftVolts > 0.1, "Left should be strictly positive to drive forward");
    assertTrue(mockIO.rightVolts > 0.1, "Right should be strictly positive to drive forward");
  }

  @Test
  void testCCWTurnCommandRoutesInverseVoltage() {
    // 0 m/s forward, 2 rad/s turn CCW (+Omega in WPILib)
    subsystem.drive(0.0, 2.0);
    subsystem.periodic();

    assertTrue(mockIO.leftVolts < -0.1, "Left should reverse for CCW turn");
    assertTrue(mockIO.rightVolts > 0.1, "Right should go forward for CCW turn");
  }

  @Test
  void testStopCommandRoutesZeroVoltage() {
    // 0 m/s forward, 0 rad/s turn
    subsystem.drive(0.0, 0.0);
    subsystem.periodic();

    assertEquals(0.0, mockIO.leftVolts, 0.001, "Left should be zero");
    assertEquals(0.0, mockIO.rightVolts, 0.001, "Right should be zero");
  }
}

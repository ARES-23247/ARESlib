package org.areslib.subsystems.drive;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.math.kinematics.ChassisSpeeds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MecanumDriveSubsystemTest {

  private static class MockMecanumIO implements MecanumDriveIO {
    public double flVolts, frVolts, blVolts, brVolts;

    @Override
    public void updateInputs(MecanumDriveIO.MecanumDriveInputs inputs) {
      // Provide dummy input data
      inputs.frontLeftVelocityMps = 0.0;
      inputs.frontRightVelocityMps = 0.0;
      inputs.rearLeftVelocityMps = 0.0;
      inputs.rearRightVelocityMps = 0.0;
    }

    @Override
    public void setVoltages(
        double frontLeft, double frontRight, double backLeft, double backRight) {
      this.flVolts = frontLeft;
      this.frVolts = frontRight;
      this.blVolts = backLeft;
      this.brVolts = backRight;
    }
  }

  private MockMecanumIO mockIO;
  private MecanumDriveSubsystem subsystem;

  @BeforeEach
  void setup() {
    mockIO = new MockMecanumIO();
    // Uses Constants.DriveConstants.MECANUM_CONFIG internally
    subsystem = new MecanumDriveSubsystem(mockIO, new MecanumDriveSubsystem.Config());
  }

  @Test
  void testForwardDriveCommandRoutesPositiveVoltage() {
    // Command 2 meters per second forward
    ChassisSpeeds targetSpeeds = new ChassisSpeeds(2.0, 0.0, 0.0);
    subsystem.drive(
        targetSpeeds.vxMetersPerSecond,
        targetSpeeds.vyMetersPerSecond,
        targetSpeeds.omegaRadiansPerSecond);
    subsystem.periodic();

    // Feedforward should request positive voltage on all 4 wheels
    assertTrue(mockIO.flVolts > 0.1, "Front Left should drive forward");
    assertTrue(mockIO.frVolts > 0.1, "Front Right should drive forward");
    assertTrue(mockIO.blVolts > 0.1, "Back Left should drive forward");
    assertTrue(mockIO.brVolts > 0.1, "Back Right should drive forward");
  }

  @Test
  void testStrafeLeftCommandRoutesCorrectVoltageSigns() {
    // Command 2 meters per second LEFT (+Y in WPILib standard)
    ChassisSpeeds targetSpeeds = new ChassisSpeeds(0.0, 2.0, 0.0);
    subsystem.drive(
        targetSpeeds.vxMetersPerSecond,
        targetSpeeds.vyMetersPerSecond,
        targetSpeeds.omegaRadiansPerSecond);
    subsystem.periodic();

    // Standard mecanum left strafe: FL/BR backwards, FR/BL forwards
    assertTrue(mockIO.flVolts < -0.1, "Front Left should pull backward to strafe left");
    assertTrue(mockIO.brVolts < -0.1, "Back Right should pull backward to strafe left");
    assertTrue(mockIO.frVolts > 0.1, "Front Right should push forward to strafe left");
    assertTrue(mockIO.blVolts > 0.1, "Back Left should push forward to strafe left");
  }

  @Test
  void testRotateCCWCommandRoutesCorrectVoltageSigns() {
    // Command 2 rad per second CCW (+Omega in WPILib standard)
    ChassisSpeeds targetSpeeds = new ChassisSpeeds(0.0, 0.0, 2.0);
    subsystem.drive(
        targetSpeeds.vxMetersPerSecond,
        targetSpeeds.vyMetersPerSecond,
        targetSpeeds.omegaRadiansPerSecond);
    subsystem.periodic();

    // Standard CCW rotation: Left wheels backwards, Right wheels forwards
    assertTrue(mockIO.flVolts < -0.1, "Front Left should pull backward to rotate CCW");
    assertTrue(mockIO.blVolts < -0.1, "Back Left should pull backward to rotate CCW");
    assertTrue(mockIO.frVolts > 0.1, "Front Right should push forward to rotate CCW");
    assertTrue(mockIO.brVolts > 0.1, "Back Right should push forward to rotate CCW");
  }
}

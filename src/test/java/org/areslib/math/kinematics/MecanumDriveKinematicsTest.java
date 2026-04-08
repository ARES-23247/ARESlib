package org.areslib.math.kinematics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;

class MecanumDriveKinematicsTest {

  private final Translation2d fl = new Translation2d(0.5, 0.5);
  private final Translation2d fr = new Translation2d(0.5, -0.5);
  private final Translation2d rl = new Translation2d(-0.5, 0.5);
  private final Translation2d rr = new Translation2d(-0.5, -0.5);

  private final MecanumDriveKinematics kinematics = new MecanumDriveKinematics(fl, fr, rl, rr);

  @Test
  void testForwardKinematics() {
    ChassisSpeeds speeds = new ChassisSpeeds(1.0, 0.0, 0.0);
    MecanumDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(speeds);

    assertEquals(1.0, wheelSpeeds.frontLeftMetersPerSecond, 0.01);
    assertEquals(1.0, wheelSpeeds.frontRightMetersPerSecond, 0.01);
    assertEquals(1.0, wheelSpeeds.rearLeftMetersPerSecond, 0.01);
    assertEquals(1.0, wheelSpeeds.rearRightMetersPerSecond, 0.01);
  }

  @Test
  void testStrafeKinematics() {
    ChassisSpeeds speeds = new ChassisSpeeds(0.0, 1.0, 0.0);
    MecanumDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(speeds);

    assertEquals(-1.0, wheelSpeeds.frontLeftMetersPerSecond, 0.01);
    assertEquals(1.0, wheelSpeeds.frontRightMetersPerSecond, 0.01);
    assertEquals(1.0, wheelSpeeds.rearLeftMetersPerSecond, 0.01);
    assertEquals(-1.0, wheelSpeeds.rearRightMetersPerSecond, 0.01);
  }

  @Test
  void testRotationKinematics() {
    ChassisSpeeds speeds = new ChassisSpeeds(0.0, 0.0, 1.0);
    MecanumDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(speeds);

    assertEquals(-1.0, wheelSpeeds.frontLeftMetersPerSecond, 0.01);
    assertEquals(1.0, wheelSpeeds.frontRightMetersPerSecond, 0.01);
    assertEquals(-1.0, wheelSpeeds.rearLeftMetersPerSecond, 0.01);
    assertEquals(1.0, wheelSpeeds.rearRightMetersPerSecond, 0.01);
  }
}

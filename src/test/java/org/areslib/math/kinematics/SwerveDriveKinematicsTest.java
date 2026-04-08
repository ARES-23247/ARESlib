package org.areslib.math.kinematics;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SwerveDriveKinematics}. */
class SwerveDriveKinematicsTest {

  private static final double EPSILON = 1e-4;

  // Standard square swerve: modules at ±0.3m
  private final SwerveDriveKinematics kinematics =
      new SwerveDriveKinematics(
          new Translation2d(0.3, 0.3), // front left
          new Translation2d(0.3, -0.3), // front right
          new Translation2d(-0.3, 0.3), // rear left
          new Translation2d(-0.3, -0.3) // rear right
          );

  @Test
  @DisplayName("Constructor requires at least 2 modules")
  void tooFewModules() {
    assertThrows(
        IllegalArgumentException.class, () -> new SwerveDriveKinematics(new Translation2d(0, 0)));
  }

  @Test
  @DisplayName("Zero chassis speeds produces zero module states")
  void zeroSpeedsZeroModules() {
    SwerveModuleState[] states = kinematics.toSwerveModuleStates(new ChassisSpeeds());
    assertEquals(4, states.length);
    for (SwerveModuleState state : states) {
      assertEquals(0.0, state.speedMetersPerSecond, EPSILON);
    }
  }

  @Test
  @DisplayName("Pure forward produces all modules forward")
  void pureForward() {
    SwerveModuleState[] states = kinematics.toSwerveModuleStates(new ChassisSpeeds(2.0, 0, 0));
    for (SwerveModuleState state : states) {
      assertEquals(2.0, state.speedMetersPerSecond, EPSILON);
      assertEquals(0.0, state.angle.getRadians(), EPSILON);
    }
  }

  @Test
  @DisplayName("Pure strafe produces all modules at 90°")
  void pureStrafe() {
    SwerveModuleState[] states = kinematics.toSwerveModuleStates(new ChassisSpeeds(0, 2.0, 0));
    for (SwerveModuleState state : states) {
      assertEquals(2.0, state.speedMetersPerSecond, EPSILON);
      assertEquals(Math.PI / 2, state.angle.getRadians(), EPSILON);
    }
  }

  @Test
  @DisplayName("Forward/inverse round-trip (pure forward)")
  void roundTripForward() {
    ChassisSpeeds original = new ChassisSpeeds(2.0, 0, 0);
    SwerveModuleState[] states = kinematics.toSwerveModuleStates(original);
    ChassisSpeeds recovered = kinematics.toChassisSpeeds(states);
    assertEquals(original.vxMetersPerSecond, recovered.vxMetersPerSecond, EPSILON);
    assertEquals(original.vyMetersPerSecond, recovered.vyMetersPerSecond, EPSILON);
    assertEquals(original.omegaRadiansPerSecond, recovered.omegaRadiansPerSecond, EPSILON);
  }

  @Test
  @DisplayName("Forward/inverse round-trip (combined motion)")
  void roundTripCombined() {
    ChassisSpeeds original = new ChassisSpeeds(1.0, 0.5, 0.3);
    SwerveModuleState[] states = kinematics.toSwerveModuleStates(original);
    ChassisSpeeds recovered = kinematics.toChassisSpeeds(states);
    assertEquals(original.vxMetersPerSecond, recovered.vxMetersPerSecond, EPSILON);
    assertEquals(original.vyMetersPerSecond, recovered.vyMetersPerSecond, EPSILON);
    assertEquals(original.omegaRadiansPerSecond, recovered.omegaRadiansPerSecond, EPSILON);
  }

  @Test
  @DisplayName("toChassisSpeeds throws on wrong number of modules")
  void wrongModuleCount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> kinematics.toChassisSpeeds(new SwerveModuleState(), new SwerveModuleState()));
  }

  @Test
  @DisplayName("desaturateWheelSpeeds scales down when needed")
  void desaturateScalesDown() {
    SwerveModuleState[] states = {
      new SwerveModuleState(10.0, new Rotation2d()),
      new SwerveModuleState(5.0, new Rotation2d()),
      new SwerveModuleState(8.0, new Rotation2d()),
      new SwerveModuleState(3.0, new Rotation2d())
    };
    SwerveDriveKinematics.desaturateWheelSpeeds(states, 4.0);
    // Max was 10, scale = 4/10 = 0.4
    assertEquals(4.0, states[0].speedMetersPerSecond, EPSILON);
    assertEquals(2.0, states[1].speedMetersPerSecond, EPSILON);
    assertEquals(3.2, states[2].speedMetersPerSecond, EPSILON);
    assertEquals(1.2, states[3].speedMetersPerSecond, EPSILON);
  }

  @Test
  @DisplayName("desaturateWheelSpeeds does nothing when under limit")
  void desaturateNoChange() {
    SwerveModuleState[] states = {
      new SwerveModuleState(2.0, new Rotation2d()), new SwerveModuleState(1.0, new Rotation2d()),
    };
    // This should fail on wrong module count for our 4-module kinematics,
    // but desaturate is static so it works on any array
    SwerveDriveKinematics.desaturateWheelSpeeds(states, 5.0);
    assertEquals(2.0, states[0].speedMetersPerSecond, EPSILON);
    assertEquals(1.0, states[1].speedMetersPerSecond, EPSILON);
  }
}

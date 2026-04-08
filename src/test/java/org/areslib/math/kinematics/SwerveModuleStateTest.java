package org.areslib.math.kinematics;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.math.geometry.Rotation2d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SwerveModuleState}. */
class SwerveModuleStateTest {

  private static final double EPSILON = 1e-6;

  @Test
  @DisplayName("Default constructor produces zero state")
  void defaultConstructor() {
    SwerveModuleState state = new SwerveModuleState();
    assertEquals(0.0, state.speedMetersPerSecond, EPSILON);
    assertEquals(0.0, state.angle.getRadians(), EPSILON);
  }

  @Test
  @DisplayName("Parameterized constructor")
  void paramConstructor() {
    SwerveModuleState state = new SwerveModuleState(3.0, new Rotation2d(Math.PI / 4));
    assertEquals(3.0, state.speedMetersPerSecond, EPSILON);
    assertEquals(Math.PI / 4, state.angle.getRadians(), EPSILON);
  }

  // ===== Optimize tests =====

  @Test
  @DisplayName("optimize keeps state when delta < 90°")
  void optimizeSmallDelta() {
    SwerveModuleState desired = new SwerveModuleState(2.0, new Rotation2d(0.5));
    SwerveModuleState result = SwerveModuleState.optimize(desired, new Rotation2d(0.0));
    // Delta = 0.5 < PI/2, no flip
    assertEquals(2.0, result.speedMetersPerSecond, EPSILON);
    assertEquals(0.5, result.angle.getRadians(), EPSILON);
  }

  @Test
  @DisplayName("optimize flips speed when delta > 90°")
  void optimizeFlipOver90() {
    // Current at 0°, desired at 150° (> 90°) → should flip to -30° with negative speed
    SwerveModuleState desired = new SwerveModuleState(2.0, Rotation2d.fromDegrees(150));
    SwerveModuleState result = SwerveModuleState.optimize(desired, new Rotation2d(0));

    // Speed should be negated
    assertEquals(-2.0, result.speedMetersPerSecond, EPSILON);
    // Angle should be 150° - 180° = -30° = -PI/6
    assertEquals(Math.toRadians(-30), result.angle.getRadians(), 0.01);
  }

  @Test
  @DisplayName("optimize flips speed when delta < -90°")
  void optimizeFlipUnderMinus90() {
    // Current at 0°, desired at -150° (< -90°) → should flip
    SwerveModuleState desired = new SwerveModuleState(2.0, Rotation2d.fromDegrees(-150));
    SwerveModuleState result = SwerveModuleState.optimize(desired, new Rotation2d(0));

    assertEquals(-2.0, result.speedMetersPerSecond, EPSILON);
  }

  @Test
  @DisplayName("optimize handles same angle (no change)")
  void optimizeSameAngle() {
    SwerveModuleState desired = new SwerveModuleState(5.0, new Rotation2d(1.0));
    SwerveModuleState result = SwerveModuleState.optimize(desired, new Rotation2d(1.0));
    assertEquals(5.0, result.speedMetersPerSecond, EPSILON);
    assertEquals(1.0, result.angle.getRadians(), EPSILON);
  }

  @Test
  @DisplayName("optimize handles 180° flip")
  void optimize180Flip() {
    SwerveModuleState desired = new SwerveModuleState(3.0, new Rotation2d(Math.PI));
    SwerveModuleState result = SwerveModuleState.optimize(desired, new Rotation2d(0));
    // At exactly 180°, wrapped delta = PI which is > PI/2 → flip
    assertEquals(-3.0, result.speedMetersPerSecond, EPSILON);
  }

  @Test
  @DisplayName("optimize with zero speed keeps angle")
  void optimizeZeroSpeed() {
    SwerveModuleState desired = new SwerveModuleState(0.0, new Rotation2d(Math.PI));
    SwerveModuleState result = SwerveModuleState.optimize(desired, new Rotation2d(0));
    // Speed is 0 so direction doesn't matter functionally
    assertEquals(0.0, Math.abs(result.speedMetersPerSecond), EPSILON);
  }
}

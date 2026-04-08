package org.areslib.math.kinematics;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DifferentialDriveOdometry}. */
class DifferentialDriveOdometryTest {

  private static final double EPSILON = 1e-4;
  private final DifferentialDriveKinematics kinematics = new DifferentialDriveKinematics(0.6);

  private DifferentialDriveOdometry createOdometry(Pose2d initialPose) {
    return new DifferentialDriveOdometry(
        kinematics,
        initialPose.getRotation(),
        new DifferentialDriveWheelPositions(0, 0),
        initialPose);
  }

  @Test
  @DisplayName("Initial pose is preserved")
  void initialPose() {
    Pose2d initial = new Pose2d(1, 2, new Rotation2d(0.5));
    DifferentialDriveOdometry odom = createOdometry(initial);
    assertEquals(1.0, odom.getPose().getX(), EPSILON);
    assertEquals(2.0, odom.getPose().getY(), EPSILON);
    assertEquals(0.5, odom.getPose().getRotation().getRadians(), EPSILON);
  }

  @Test
  @DisplayName("Default initial pose is origin")
  void defaultPose() {
    DifferentialDriveOdometry odom =
        new DifferentialDriveOdometry(
            kinematics, new Rotation2d(), new DifferentialDriveWheelPositions(0, 0));
    assertEquals(0.0, odom.getPose().getX(), EPSILON);
    assertEquals(0.0, odom.getPose().getY(), EPSILON);
  }

  @Test
  @DisplayName("Pure forward motion along X axis")
  void pureForward() {
    DifferentialDriveOdometry odom = createOdometry(new Pose2d());
    // Both wheels advance 1m, gyro stays at 0
    odom.update(new Rotation2d(0), new DifferentialDriveWheelPositions(1, 1));
    assertEquals(1.0, odom.getPose().getX(), EPSILON);
    assertEquals(0.0, odom.getPose().getY(), EPSILON);
  }

  @Test
  @DisplayName("Multiple forward updates accumulate")
  void cumulativeForward() {
    DifferentialDriveOdometry odom = createOdometry(new Pose2d());
    odom.update(new Rotation2d(0), new DifferentialDriveWheelPositions(0.5, 0.5));
    odom.update(new Rotation2d(0), new DifferentialDriveWheelPositions(1.0, 1.0));
    assertEquals(1.0, odom.getPose().getX(), EPSILON);
  }

  @Test
  @DisplayName("Turn in place changes heading but not position")
  void turnInPlace() {
    DifferentialDriveOdometry odom = createOdometry(new Pose2d());
    // Wheels go opposite directions for rotation
    double arcLength = 0.3 * Math.PI / 2; // Track/2 * angle
    odom.update(
        new Rotation2d(Math.PI / 2), new DifferentialDriveWheelPositions(-arcLength, arcLength));
    assertEquals(Math.PI / 2, odom.getPose().getRotation().getRadians(), EPSILON);
    // Position should barely change (small drift from arc geometry)
    assertEquals(0.0, odom.getPose().getX(), 0.05);
  }

  @Test
  @DisplayName("resetPosition resets everything")
  void resetPosition() {
    DifferentialDriveOdometry odom = createOdometry(new Pose2d());
    odom.update(new Rotation2d(0), new DifferentialDriveWheelPositions(5, 5));

    Pose2d newPose = new Pose2d(10, 20, new Rotation2d(1.0));
    odom.resetPosition(new Rotation2d(1.0), new DifferentialDriveWheelPositions(0, 0), newPose);
    assertEquals(10.0, odom.getPose().getX(), EPSILON);
    assertEquals(20.0, odom.getPose().getY(), EPSILON);
  }

  @Test
  @DisplayName("resetTranslation only changes pose, not wheel state")
  void resetTranslation() {
    DifferentialDriveOdometry odom = createOdometry(new Pose2d());
    odom.update(new Rotation2d(0), new DifferentialDriveWheelPositions(1, 1));

    odom.resetTranslation(new Pose2d(99, 99, new Rotation2d()));
    assertEquals(99.0, odom.getPose().getX(), EPSILON);
  }

  @Test
  @DisplayName("Forward motion with rotation follows arc")
  void arcMotion() {
    DifferentialDriveOdometry odom = createOdometry(new Pose2d());
    // Robot drives forward while turning 90° left
    odom.update(new Rotation2d(Math.PI / 2), new DifferentialDriveWheelPositions(0.5, 1.5));
    // Should have moved both in X and Y
    assertTrue(odom.getPose().getX() > 0);
    assertTrue(odom.getPose().getY() > 0);
  }
}

package org.areslib.math.estimator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Twist2d;
import org.areslib.math.kinematics.DifferentialDriveKinematics;
import org.areslib.math.kinematics.DifferentialDriveWheelPositions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DifferentialDrivePoseEstimatorTest {

  private static final double EPSILON = 1e-6;
  private DifferentialDriveKinematics mockKinematics;
  private DifferentialDrivePoseEstimator estimator;

  @BeforeEach
  void setUp() {
    mockKinematics = Mockito.mock(DifferentialDriveKinematics.class);

    DifferentialDriveWheelPositions initialPositions =
        new DifferentialDriveWheelPositions(0.0, 0.0);
    Pose2d initialPose = new Pose2d(0.0, 0.0, new Rotation2d(0.0));

    estimator =
        new DifferentialDrivePoseEstimator(
            mockKinematics, new Rotation2d(0.0), initialPositions, initialPose);
  }

  @Test
  @DisplayName("Estimator starts at initial estimated pose")
  void startsAtInitialPose() {
    Pose2d currentPos = estimator.getEstimatedPosition();
    assertEquals(0.0, currentPos.getX(), EPSILON);
    assertEquals(0.0, currentPos.getY(), EPSILON);
  }

  @Test
  @DisplayName("Update applies kinematic twist delta")
  void updateAppliesKinematicStep() {
    when(mockKinematics.toTwist2d(any(), any())).thenReturn(new Twist2d(1.0, 0.0, 0.0));

    DifferentialDriveWheelPositions newPositions = new DifferentialDriveWheelPositions(1.0, 1.0);

    Pose2d newEstimatedPos = estimator.update(new Rotation2d(0.0), newPositions, 0.5);

    assertEquals(1.0, newEstimatedPos.getX(), EPSILON);
    assertEquals(1.0, estimator.getEstimatedPosition().getX(), EPSILON);
  }

  @Test
  @DisplayName("Vision measurement applies correctly with history buffer")
  void visionMeasurementAppliesCorrectly() {
    // Step 1: Move from X=0 to X=1 at t=0.5
    when(mockKinematics.toTwist2d(any(), any())).thenReturn(new Twist2d(1.0, 0.0, 0.0));
    estimator.update(new Rotation2d(0.0), new DifferentialDriveWheelPositions(1.0, 1.0), 0.5);

    // Step 2: Move from X=1 to X=2 at t=1.0
    when(mockKinematics.toTwist2d(any(), any())).thenReturn(new Twist2d(1.0, 0.0, 0.0));
    estimator.update(new Rotation2d(0.0), new DifferentialDriveWheelPositions(2.0, 2.0), 1.0);

    // Current estimator is at X=2.0
    // Vision says at t=0.5 we were at X=1.5
    // Re-applying to history creates corrected base pose, plus the replay from t=0.5 to t=1.0 (+1.0
    // in X)
    // With stdDevs default [0.1, 0.1, 0.1], weight is ~0.909
    // Corrected at t=0.5: 1.0 + (1.5 - 1.0) * 0.909 = 1.4545
    // Replay: 1.4545 + 1.0 = 2.4545
    Pose2d visionPose = new Pose2d(1.5, 0.0, new Rotation2d(0.0));
    estimator.addVisionMeasurement(visionPose, 0.5);

    Pose2d correctedPose = estimator.getEstimatedPosition();
    assertTrue(
        correctedPose.getX() > 2.4 && correctedPose.getX() < 2.50,
        "Pose X should be around 2.45 after fusion but was " + correctedPose.getX());
  }

  @Test
  @DisplayName("Resetting position zero-outs the odometry states")
  void resetPositionClearsState() {
    when(mockKinematics.toTwist2d(any(), any())).thenReturn(new Twist2d(5.0, 0.0, 0.0));
    estimator.update(new Rotation2d(0.0), new DifferentialDriveWheelPositions(5.0, 5.0), 1.0);

    estimator.resetPosition(
        new Rotation2d(0.0),
        new DifferentialDriveWheelPositions(0.0, 0.0),
        new Pose2d(10.0, 10.0, new Rotation2d(0.0)));

    Pose2d newEstimated = estimator.getEstimatedPosition();
    assertEquals(10.0, newEstimated.getX(), EPSILON);
    assertEquals(10.0, newEstimated.getY(), EPSILON);
  }
}

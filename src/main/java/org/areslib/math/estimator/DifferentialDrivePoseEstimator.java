package org.areslib.math.estimator;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.TimeInterpolatableBuffer;
import org.areslib.math.kinematics.DifferentialDriveKinematics;
import org.areslib.math.kinematics.DifferentialDriveOdometry;
import org.areslib.math.kinematics.DifferentialDriveWheelPositions;

/**
 * A sophisticated pose estimator for Differential Drives mirroring elite algorithms (WPILib 2024).
 * Uses exact module position tracking and a History Buffer for true latency-compensating Vision
 * fusion.
 */
public class DifferentialDrivePoseEstimator {
  private final DifferentialDriveOdometry odometry;
  private final TimeInterpolatableBuffer<Pose2d> poseBuffer;

  private Pose2d estimatedPose;
  private double[] visionStdDevs = new double[] {0.1, 0.1, 0.1};

  /**
   * Constructs a DifferentialDrivePoseEstimator.
   *
   * @param kinematics A correctly-configured kinematics object for your drivetrain.
   * @param gyroAngle The current gyro angle.
   * @param wheelPositions The initial differential wheel positions.
   * @param initialPoseMeters The starting pose estimate.
   */
  public DifferentialDrivePoseEstimator(
      DifferentialDriveKinematics kinematics,
      Rotation2d gyroAngle,
      DifferentialDriveWheelPositions wheelPositions,
      Pose2d initialPoseMeters) {
    odometry =
        new DifferentialDriveOdometry(kinematics, gyroAngle, wheelPositions, initialPoseMeters);
    estimatedPose = initialPoseMeters;

    poseBuffer = TimeInterpolatableBuffer.createBuffer(75);
  }

  public void setVisionMeasurementStdDevs(double[] visionStdDevs) {
    if (visionStdDevs.length != 3) {
      throw new IllegalArgumentException("Standard deviations array must be of length 3");
    }
    this.visionStdDevs = visionStdDevs;
  }

  public void resetPosition(
      Rotation2d gyroAngle, DifferentialDriveWheelPositions wheelPositions, Pose2d poseMeters) {
    odometry.resetPosition(gyroAngle, wheelPositions, poseMeters);
    estimatedPose = poseMeters;
    poseBuffer.clear();
  }

  public Pose2d getEstimatedPosition() {
    return estimatedPose;
  }

  /**
   * Updates the pose estimator with exactly tracked differential wheel positions.
   *
   * @param gyroAngle The current gyro angle.
   * @param wheelPositions The current wheel positions.
   * @param timestampSeconds the exact time the modules were sampled.
   * @return The updated estimated pose.
   */
  public Pose2d update(
      Rotation2d gyroAngle,
      DifferentialDriveWheelPositions wheelPositions,
      double timestampSeconds) {
    estimatedPose = odometry.update(gyroAngle, wheelPositions);
    poseBuffer.addSample(timestampSeconds, estimatedPose);
    return estimatedPose;
  }

  /**
   * Adds a vision measurement to the pose estimator. Rolls back Odometry in time, applying the
   * vision measurement when the photo was literally taken, and replays forward.
   *
   * @param visionRobotPoseMeters The pose of the robot as measured by the vision camera.
   * @param timestampSeconds The precise time the photo was taken (e.g. current_time -
   *     pipeline_latency).
   */
  public void addVisionMeasurement(Pose2d visionRobotPoseMeters, double timestampSeconds) {
    estimatedPose =
        VisionFusionHelper.applyVisionMeasurement(
            visionRobotPoseMeters, timestampSeconds, estimatedPose, poseBuffer, visionStdDevs);

    odometry.resetTranslation(estimatedPose);
  }
}

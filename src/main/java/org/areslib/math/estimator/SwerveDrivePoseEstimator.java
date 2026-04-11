package org.areslib.math.estimator;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.TimeInterpolatableBuffer;
import org.areslib.math.kinematics.SwerveDriveKinematics;
import org.areslib.math.kinematics.SwerveDriveOdometry;
import org.areslib.math.kinematics.SwerveModulePosition;

/**
 * A sophisticated pose estimator for Swerve Drives mirroring elite algorithms (WPILib 2024). Uses
 * exact module position tracking and a History Buffer for true latency-compensating Vision fusion.
 */
public class SwerveDrivePoseEstimator {
  private final SwerveDriveOdometry odometry;
  private final TimeInterpolatableBuffer<Pose2d> poseBuffer;

  private Pose2d estimatedPose;
  private double[] visionStdDevs = new double[] {0.1, 0.1, 0.1};

  /**
   * Constructs a SwerveDrivePoseEstimator.
   *
   * @param kinematics A correctly-configured kinematics object for your drivetrain.
   * @param gyroAngle The current gyro angle.
   * @param modulePositions The initial swerve module positions.
   * @param initialPoseMeters The starting pose estimate.
   */
  public SwerveDrivePoseEstimator(
      SwerveDriveKinematics kinematics,
      Rotation2d gyroAngle,
      SwerveModulePosition[] modulePositions,
      Pose2d initialPoseMeters) {
    odometry = new SwerveDriveOdometry(kinematics, gyroAngle, modulePositions, initialPoseMeters);
    estimatedPose = initialPoseMeters;

    // Elite teams track ~1.5s of history. 50 loops/sec * 1.5 = 75 loops.
    poseBuffer = TimeInterpolatableBuffer.createBuffer(75);
  }

  public void setVisionMeasurementStdDevs(double[] visionStdDevs) {
    if (visionStdDevs.length != 3) {
      throw new IllegalArgumentException("Standard deviations array must be of length 3");
    }
    this.visionStdDevs = visionStdDevs;
  }

  public void resetPosition(
      Rotation2d gyroAngle, SwerveModulePosition[] modulePositions, Pose2d poseMeters) {
    odometry.resetPosition(gyroAngle, modulePositions, poseMeters);
    estimatedPose = poseMeters;
    poseBuffer.clear();
  }

  public Pose2d getEstimatedPosition() {
    return estimatedPose;
  }

  /**
   * Updates the pose estimator with exactly tracked swerve module positions.
   *
   * @param gyroAngle The current gyro angle.
   * @param modulePositions The current module positions.
   * @param timestampSeconds the exact time the modules were sampled.
   * @return The updated estimated pose.
   */
  public Pose2d update(
      Rotation2d gyroAngle, SwerveModulePosition[] modulePositions, double timestampSeconds) {
    estimatedPose = odometry.update(gyroAngle, modulePositions);
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
    addVisionMeasurement(visionRobotPoseMeters, timestampSeconds, visionStdDevs);
  }

  /**
   * Adds a vision measurement to the pose estimator, using instantaneous dynamic standard
   * deviations.
   *
   * @param visionRobotPoseMeters The pose of the robot as measured by the vision camera.
   * @param timestampSeconds The precise time the photo was taken
   * @param visionStdDevs Instantaneous standard deviations [x, y, theta] based on
   *     distance/ambiguity scaling.
   */
  public void addVisionMeasurement(
      Pose2d visionRobotPoseMeters, double timestampSeconds, double[] visionStdDevs) {
    estimatedPose =
        VisionFusionHelper.applyVisionMeasurement(
            visionRobotPoseMeters, timestampSeconds, estimatedPose, poseBuffer, visionStdDevs);

    // Sync Odometry's internal pose without destroying wheel buffers:
    odometry.resetTranslation(estimatedPose);
  }
}

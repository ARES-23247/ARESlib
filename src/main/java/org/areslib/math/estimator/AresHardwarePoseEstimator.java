package org.areslib.math.estimator;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.TimeInterpolatableBuffer;
import org.areslib.math.geometry.Twist2d;

/**
 * A highly specialized Pose Estimator designed exclusively for external hardware odometry boards
 * (e.g. GoBilda Pinpoint). It mathematically extracts the Twist differentials from consecutive
 * global hardware reports and projects them forwards across delayed vision inputs without requiring
 * rigid WPILib mechanical kinematics implementations.
 */
public class AresHardwarePoseEstimator {
  private final TimeInterpolatableBuffer<Pose2d> poseBuffer;

  private Pose2d estimatedPose;
  private Pose2d previousHardwarePose;
  private double[] visionStdDevs = new double[] {0.1, 0.1, 0.1};

  /**
   * Constructs a generic Hardware Pose Estimator.
   *
   * @param initialHardwarePose The very first pose read from the hardware.
   * @param initialEstimatedPose The starting pose of the robot on the field map.
   */
  public AresHardwarePoseEstimator(Pose2d initialHardwarePose, Pose2d initialEstimatedPose) {
    previousHardwarePose = initialHardwarePose;
    estimatedPose = initialEstimatedPose;
    // Buffer up to 1.5 seconds of data dynamically at ~50Hz
    poseBuffer = TimeInterpolatableBuffer.createBuffer(75);
  }

  /**
   * Updates the standard deviations for Vision Kalman filter scaling.
   *
   * @param visionStdDevs Range scaling inputs [x, y, theta]
   */
  public void setVisionMeasurementStdDevs(double[] visionStdDevs) {
    if (visionStdDevs.length != 3) {
      throw new IllegalArgumentException("Standard deviations array must be of length 3");
    }
    this.visionStdDevs = visionStdDevs;
  }

  /**
   * Resets the entire underlying track system to a new starting geometry location.
   *
   * @param hardwarePose The current raw position as determined natively by the tracking device.
   * @param estimatedPose The global location where you physically want the robot set to.
   */
  public void resetPosition(Pose2d hardwarePose, Pose2d estimatedPose) {
    previousHardwarePose = hardwarePose;
    this.estimatedPose = estimatedPose;
    poseBuffer.clear();
  }

  /**
   * Returns the globally-corrected location of the robot on the mapping axis.
   *
   * @return The globally-corrected location.
   */
  public Pose2d getEstimatedPosition() {
    return estimatedPose;
  }

  /**
   * Primary loop update parameter. Should be fed with hardware reads continuously.
   *
   * @param currentHardwarePose The exact pose the external tracker thinks the robot is at globally
   *     right now.
   * @param timestampSeconds The precise JVM loop time of this calculation.
   * @return The updated true mapped estimator position natively combined with vision history.
   */
  public Pose2d update(Pose2d currentHardwarePose, double timestampSeconds) {
    // Find the exact geometric curve (Twist2d arc) mapping the last hardware loop to right now
    Twist2d deltaTwist = previousHardwarePose.log(currentHardwarePose);

    // Mathematically project that curve forward off of our corrected Vision origin map
    estimatedPose = estimatedPose.exp(deltaTwist);

    // Cache the hardware state locally for the next delta pull
    previousHardwarePose = currentHardwarePose;

    // Commit to JVM history timeline mapping for retro-play tracking
    poseBuffer.addSample(timestampSeconds, estimatedPose);

    return estimatedPose;
  }

  /**
   * Injects camera offset measurements, retroactively applies them physically backward in time to
   * precisely overlay exact arc motions, and adjusts the estimator globally.
   *
   * @param visionRobotPoseMeters The absolute position the AprilTag/CV math puts camera.
   * @param timestampSeconds The explicit timestamp the image was captured minus camera-pipeline
   *     delay.
   */
  public void addVisionMeasurement(Pose2d visionRobotPoseMeters, double timestampSeconds) {
    addVisionMeasurement(visionRobotPoseMeters, timestampSeconds, visionStdDevs);
  }

  /**
   * Injects camera offset measurements using instantaneous dynamic standard deviations.
   *
   * @param visionRobotPoseMeters The absolute position the AprilTag/CV math puts camera.
   * @param timestampSeconds The explicit timestamp the image was captured
   * @param visionStdDevs Instantaneous standard deviations [x, y, theta]
   */
  public void addVisionMeasurement(
      Pose2d visionRobotPoseMeters, double timestampSeconds, double[] visionStdDevs) {
    estimatedPose =
        VisionFusionHelper.applyVisionMeasurement(
            visionRobotPoseMeters, timestampSeconds, estimatedPose, poseBuffer, visionStdDevs);
  }
}

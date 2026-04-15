package org.areslib.hardware.interfaces;

import org.areslib.telemetry.AresLoggableInputs;

/**
 * AdvantageKit-style IO abstraction for robust vision processing. This can be implemented by a real
 * Limelight/PhotonVision camera or a simulated environment.
 */
public interface VisionIO {

  /**
   * Loggable data object containing vision processing state, including target offsets and 3D pose
   * estimates.
   */
  @org.areslib.telemetry.AresAutoLogger.AutoLog
  class VisionInputs implements AresLoggableInputs {
    /** True if the vision processor currently sees a valid target. */
    public boolean hasTarget = false;

    /** Offset of the primary target in the X axis (degrees). */
    public double tx = 0.0;

    /** Offset of the primary target in the Y axis (degrees). */
    public double ty = 0.0;

    /** Area of the primary target bounding box (% of image). */
    public double ta = 0.0;

    /**
     * Struct array for the primary Robot Pose calculation (if computing AprilTags). Format matches
     * AdvantageScope 3D Poses (Quaternion): [x, y, z, w, i, j, k] in meters and quaternions.
     */
    public double[] botPose3d = new double[7];

    /** Optional secondary Pose structure (e.g. Megatag2). [x, y, z, w, i, j, k] */
    public double[] botPoseMegaTag2 = new double[7];

    /** Network or pipeline latency overhead in milliseconds. */
    public double latencyMs = 0.0;

    /** The ID of the currently loaded vision pipeline. */
    public int pipelineIndex = 0;

    /** Number of active fiducial markers (AprilTags) in frame. */
    public int fiducialCount = 0;

    /** Lowest ambiguity of any seen tag (0.0 to 1.0). Lower is better. */
    public double minTagAmbiguity = 0.0;

    /** Average distance to all visible tags in meters. */
    public double avgTagDistanceMeters = 0.0;

    /** True if the active botPose3d relies on gyro-seeding (e.g. Megatag2 style algorithms). */
    public boolean isMegatag2 = false;

    /**
     * Raw packed array of all individual camera 3D poses (if using multiple cameras). Layout:
     * [x,y,z,w,i,j,k] stacked sequentially. Length will be N * 7.
     */
    public double[] rawCameraPoses = new double[0];

    /** Array representing the 3D FOV frustum of the camera. Packed [x,y,z,w,i,j,k,...] */
    public double[] cameraFovFrustum = new double[0];
  }

  /**
   * Updates the data structure with the latest values from the underlying hardware sensor or
   * simulation.
   *
   * @param inputs The VisionInputs object to be populated.
   */
  default void updateInputs(VisionInputs inputs) {}

  /**
   * Changes the active vision pipeline execution index.
   *
   * @param index The index of the pipeline to switch to.
   */
  default void setPipeline(int index) {}

  /**
   * Pushes the robot's high-precision IMU yaw to the vision sensor. Required for ambiguity-free
   * MegaTag 2.0 solvers to function.
   *
   * @param yaw The field-centric rotation of the robot.
   */
  default void updateRobotOrientation(org.areslib.math.geometry.Rotation2d yaw) {}
}

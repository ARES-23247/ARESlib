package org.areslib.hardware.wrappers;

import java.util.Random;
import java.util.function.Supplier;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.hardware.interfaces.VisionIO;

/**
 * ArrayVisionIOSim standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * ArrayVisionIOSim}. Extracted and compiled as part of the ARESLib2 Code Audit for missing
 * documentation coverage.
 */
public class ArrayVisionIOSim implements VisionIO {

  private final Supplier<OdometryIO.OdometryInputs> odometrySupplier;
  private final Random rand = new Random();

  // FTC Into The Deep Wall Tag Coordinates (x, y, tag_yaw_facing)
  // Meters + Radians, center-origin coordinate system.
  // Positions approximate the center of each field wall, using the half-field size from
  // FieldConstants.
  private static final double WALL_TAG_OFFSET = org.areslib.core.FieldConstants.HALF_FIELD_METERS;
  private final double[][] intoTheDeepTags = {
    {WALL_TAG_OFFSET, 0.0, Math.PI}, // East wall tag (facing West)
    {-WALL_TAG_OFFSET, 0.0, 0.0}, // West wall tag (facing East)
    {0.0, WALL_TAG_OFFSET, -Math.PI / 2.0}, // North wall tag (facing South)
    {0.0, -WALL_TAG_OFFSET, Math.PI / 2.0} // South wall tag (facing North)
  };

  private static final double MAX_VIEW_DISTANCE_METERS = 3.5;
  private static final double CAMERA_FOV_RADIANS = Math.toRadians(75.0);

  public ArrayVisionIOSim(Supplier<OdometryIO.OdometryInputs> odometrySupplier) {
    this.odometrySupplier = odometrySupplier;
  }

  @Override
  public void updateInputs(VisionInputs inputs) {
    OdometryIO.OdometryInputs odo = odometrySupplier.get();
    if (odo == null) {
      inputs.hasTarget = false;
      return;
    }

    double robotX = odo.xMeters;
    double robotY = odo.yMeters;
    double robotHeading = odo.headingRadians;

    boolean seesTag = false;
    double closestDist = Double.MAX_VALUE;

    // Simulate Limelight scanning for Into The Deep fiducials
    for (double[] tag : intoTheDeepTags) {
      double tagX = tag[0];
      double tagY = tag[1];
      double tagHeading = tag[2];

      double distToTag = Math.hypot(tagX - robotX, tagY - robotY);
      if (distToTag > MAX_VIEW_DISTANCE_METERS) {
        continue;
      }

      // Calculate the angle from the robot to the tag
      double angleToTag = Math.atan2(tagY - robotY, tagX - robotX);

      // Shortest angular distance between where the robot is facing, and where the tag is
      double angleDifference = angleToTag - robotHeading;
      angleDifference = Math.atan2(Math.sin(angleDifference), Math.cos(angleDifference));

      // Check if the tag is within the camera's Field of View
      if (Math.abs(angleDifference) <= CAMERA_FOV_RADIANS / 2.0) {
        // Must also ensure the robot is facing the tag's FRONT face, not its back.
        // If angle from tag to robot is within 90deg of tag's heading.
        double angleFromTagToRobot = Math.atan2(robotY - tagY, robotX - tagX);
        double faceAngleDiff = angleFromTagToRobot - tagHeading;
        faceAngleDiff = Math.atan2(Math.sin(faceAngleDiff), Math.cos(faceAngleDiff));

        if (Math.abs(faceAngleDiff) <= Math.PI / 2.0) {
          if (distToTag < closestDist) {
            seesTag = true;
            closestDist = distToTag;
          }
        }
      }
    }

    inputs.hasTarget = seesTag;

    if (seesTag) {
      // Noise factor increases the further away the tag is (e.g. 1cm noise at 1m, 3cm at 3m)
      double noiseScale = 0.01 * closestDist;

      // Random gaussian distribution for natural jitter
      double noiseX = rand.nextGaussian() * noiseScale;
      double noiseY = rand.nextGaussian() * noiseScale;
      double noiseYaw = rand.nextGaussian() * (0.005 * closestDist); // tiny angular noise

      // Euler to Quaternion [w, x, y, z] formulation
      double roll = 0.0;
      double pitch = 0.0;
      double yaw = robotHeading + noiseYaw;

      double cr = Math.cos(roll * 0.5);
      double sr = Math.sin(roll * 0.5);
      double cp = Math.cos(pitch * 0.5);
      double sp = Math.sin(pitch * 0.5);
      double cy = Math.cos(yaw * 0.5);
      double sy = Math.sin(yaw * 0.5);

      // Inject true pose + generated noise
      inputs.botPose3d[0] = robotX + noiseX;
      inputs.botPose3d[1] = robotY + noiseY;
      inputs.botPose3d[2] = 0.0; // z height
      inputs.botPose3d[3] = cr * cp * cy + sr * sp * sy; // W
      inputs.botPose3d[4] = sr * cp * cy - cr * sp * sy; // X
      inputs.botPose3d[5] = cr * sp * cy + sr * cp * sy; // Y
      inputs.botPose3d[6] = cr * cp * sy - sr * sp * cy; // Z

      inputs.rawCameraPoses = new double[7];
      System.arraycopy(inputs.botPose3d, 0, inputs.rawCameraPoses, 0, 7);

      inputs.ta = Math.max(0.1, 10.0 - (closestDist * 2.0)); // Larger area when closer
      inputs.fiducialCount = 1;
      inputs.latencyMs = 12.5; // Simulate pipeline latency
    } else {
      inputs.rawCameraPoses = new double[0];
      inputs.fiducialCount = 0;
      inputs.ta = 0;
      inputs.latencyMs = 0;
    }
  }
}

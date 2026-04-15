package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;

/**
 * Universal Odometry class that integrates ChassisSpeeds and Gyro angles to estimate the robot's
 * Pose2d on the field.
 *
 * <p>Elite Feature: Serves as a standard, purely mathematical fallback when Advanced
 * visual/dead-wheel localizers (like PathPlanner) are disabled or unavailable.
 */
public class AresOdometry {
  private Pose2d pose;
  private Rotation2d previousAngle;

  public AresOdometry(Pose2d initialPose) {
    this.pose = initialPose.copy();
    this.previousAngle = initialPose.getRotation().copy();
  }

  public AresOdometry() {
    this(new Pose2d());
  }

  /**
   * Resets the robot's position on the field.
   *
   * @param pose The new pose to reset to.
   */
  public void resetPosition(Pose2d pose) {
    this.pose.set(pose);
    this.previousAngle.set(pose.getRotation());
  }

  /**
   * Updates the robot's position based on its measured speeds and passed time.
   *
   * @param gyroAngle The absolute angle from the IMU.
   * @param speeds The localized ChassisSpeeds (forward, strafe, rotational velocity).
   * @param dtSeconds The time passed since the last update (in seconds).
   * @return The updated Pose2d.
   */
  public Pose2d update(Rotation2d gyroAngle, ChassisSpeeds speeds, double dtSeconds) {
    // Delta angle from gyro (more accurate than integrating wheel odometry omega)
    double dtheta =
        org.areslib.math.MathUtil.angleModulus(gyroAngle.getRadians() - previousAngle.getRadians());

    // Standard Pose Exponential Integration (dx, dy over an arc)
    double dx = speeds.vxMetersPerSecond * dtSeconds;
    double dy = speeds.vyMetersPerSecond * dtSeconds;

    double deltaX;
    double deltaY;
    if (Math.abs(dtheta) < org.areslib.math.MathUtil.EPSILON) {
      // Straight line
      deltaX = dx;
      deltaY = dy;
    } else {
      // Arc
      double sinTheta = Math.sin(dtheta);
      double cosTheta = Math.cos(dtheta);

      double sinCoefficient = sinTheta / dtheta;
      double cosCoefficient = (1.0 - cosTheta) / dtheta;

      deltaX = dx * sinCoefficient - dy * cosCoefficient;
      deltaY = dx * cosCoefficient + dy * sinCoefficient;
    }

    // Rotate the delta according to the previous heading to get field-centric changes
    double pCos = previousAngle.getCos();
    double pSin = previousAngle.getSin();
    double fieldDeltaX = deltaX * pCos - deltaY * pSin;
    double fieldDeltaY = deltaX * pSin + deltaY * pCos;

    // Update global pose in-place
    pose.set(pose.getX() + fieldDeltaX, pose.getY() + fieldDeltaY, gyroAngle.getRadians());
    previousAngle.set(gyroAngle);
    return pose;
  }

  public Pose2d getPose() {
    return pose;
  }
}

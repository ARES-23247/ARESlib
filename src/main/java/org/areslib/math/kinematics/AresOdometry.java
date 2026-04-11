package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;

/**
 * Universal Odometry class that integrates ChassisSpeeds and Gyro angles to estimate the robot's
 * Pose2d on the field.
 *
 * <p>Elite Feature: Serves as a standard, purely mathematical fallback when Advanced
 * visual/dead-wheel localizers (like PathPlanner) are disabled or unavailable.
 */
public class AresOdometry {
  private Pose2d m_pose;
  private Rotation2d m_previousAngle;

  public AresOdometry(Pose2d initialPose) {
    m_pose = initialPose;
    m_previousAngle = initialPose.getRotation();
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
    m_pose = pose;
    m_previousAngle = pose.getRotation();
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
    double dtheta = gyroAngle.minus(m_previousAngle).getRadians();

    // Standard Pose Exponential Integration (dx, dy over an arc)
    double dx = speeds.vxMetersPerSecond * dtSeconds;
    double dy = speeds.vyMetersPerSecond * dtSeconds;

    double deltaX;
    double deltaY;
    if (Math.abs(dtheta) < 1E-9) {
      // Straight line
      deltaX = dx;
      deltaY = dy;
    } else {
      // Arc
      double sinTheta = Math.sin(dtheta);
      double cosTheta = Math.cos(dtheta);

      double s = sinTheta / dtheta;
      double c = (1.0 - cosTheta) / dtheta;

      deltaX = dx * s - dy * c;
      deltaY = dx * c + dy * s;
    }

    // Rotate the delta according to the previous heading to get field-centric changes
    double pCos = m_previousAngle.getCos();
    double pSin = m_previousAngle.getSin();
    double fieldDeltaX = deltaX * pCos - deltaY * pSin;
    double fieldDeltaY = deltaX * pSin + deltaY * pCos;

    // Update global pose (we still allocate one new Pose2d so callers don't get aliased references)
    m_pose =
        new Pose2d(
            new Translation2d(m_pose.getX() + fieldDeltaX, m_pose.getY() + fieldDeltaY), gyroAngle);
    m_previousAngle = gyroAngle;

    return m_pose;
  }

  public Pose2d getPose() {
    return m_pose;
  }
}

package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Twist2d;

public class DifferentialDriveOdometry {
  private final DifferentialDriveKinematics m_kinematics;
  private Pose2d m_pose;
  private Rotation2d m_previousAngle;
  private DifferentialDriveWheelPositions m_previousWheelPositions;

  public DifferentialDriveOdometry(
      DifferentialDriveKinematics kinematics,
      Rotation2d gyroAngle,
      DifferentialDriveWheelPositions wheelPositions,
      Pose2d initialPose) {
    m_kinematics = kinematics;
    m_pose = initialPose;
    m_previousAngle = gyroAngle;
    m_previousWheelPositions =
        new DifferentialDriveWheelPositions(wheelPositions.leftMeters, wheelPositions.rightMeters);
  }

  public DifferentialDriveOdometry(
      DifferentialDriveKinematics kinematics,
      Rotation2d gyroAngle,
      DifferentialDriveWheelPositions wheelPositions) {
    this(kinematics, gyroAngle, wheelPositions, new Pose2d());
  }

  public void resetPosition(
      Rotation2d gyroAngle, DifferentialDriveWheelPositions wheelPositions, Pose2d pose) {
    m_pose = pose;
    m_previousAngle = gyroAngle;
    m_previousWheelPositions =
        new DifferentialDriveWheelPositions(wheelPositions.leftMeters, wheelPositions.rightMeters);
  }

  /**
   * Resets the robot's pose translation without disrupting internal kinematic wheel buffers.
   * Necessary for Vision Estimators.
   *
   * @param pose The new pose of the robot.
   */
  public void resetTranslation(Pose2d pose) {
    m_pose = pose;
  }

  public Pose2d getPose() {
    return m_pose;
  }

  public Pose2d update(Rotation2d gyroAngle, DifferentialDriveWheelPositions wheelPositions) {
    Twist2d twist = m_kinematics.toTwist2d(m_previousWheelPositions, wheelPositions);

    twist.dtheta = gyroAngle.minus(m_previousAngle).getRadians();

    m_pose = m_pose.exp(twist);

    m_previousAngle = gyroAngle;
    m_previousWheelPositions =
        new DifferentialDriveWheelPositions(wheelPositions.leftMeters, wheelPositions.rightMeters);

    return m_pose;
  }
}

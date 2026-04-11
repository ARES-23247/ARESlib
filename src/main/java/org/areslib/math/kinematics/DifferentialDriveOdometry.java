package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Twist2d;

public class DifferentialDriveOdometry {
  private final DifferentialDriveKinematics kinematics;
  private Pose2d pose;
  private Rotation2d previousAngle;
  private DifferentialDriveWheelPositions previousWheelPositions;

  public DifferentialDriveOdometry(
      DifferentialDriveKinematics kinematics,
      Rotation2d gyroAngle,
      DifferentialDriveWheelPositions wheelPositions,
      Pose2d initialPose) {
    this.kinematics = kinematics;
    pose = initialPose;
    previousAngle = gyroAngle;
    previousWheelPositions =
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
    this.pose = pose;
    previousAngle = gyroAngle;
    previousWheelPositions =
        new DifferentialDriveWheelPositions(wheelPositions.leftMeters, wheelPositions.rightMeters);
  }

  /**
   * Resets the robot's pose translation without disrupting internal kinematic wheel buffers.
   * Necessary for Vision Estimators.
   *
   * @param pose The new pose of the robot.
   */
  public void resetTranslation(Pose2d pose) {
    this.pose = pose;
  }

  public Pose2d getPose() {
    return pose;
  }

  public Pose2d update(Rotation2d gyroAngle, DifferentialDriveWheelPositions wheelPositions) {
    Twist2d twist = kinematics.toTwist2d(previousWheelPositions, wheelPositions);

    twist.dtheta = gyroAngle.minus(previousAngle).getRadians();

    pose = pose.exp(twist);

    previousAngle = gyroAngle;
    previousWheelPositions =
        new DifferentialDriveWheelPositions(wheelPositions.leftMeters, wheelPositions.rightMeters);

    return pose;
  }
}

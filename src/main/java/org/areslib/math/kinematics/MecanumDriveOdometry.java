package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Twist2d;

public class MecanumDriveOdometry {
  private final MecanumDriveKinematics kinematics;
  private Pose2d pose;
  private Rotation2d previousAngle;
  private MecanumDriveWheelPositions previousWheelPositions;

  public MecanumDriveOdometry(
      MecanumDriveKinematics kinematics,
      Rotation2d gyroAngle,
      MecanumDriveWheelPositions wheelPositions,
      Pose2d initialPose) {
    this.kinematics = kinematics;
    pose = initialPose;
    previousAngle = gyroAngle;
    previousWheelPositions =
        new MecanumDriveWheelPositions(
            wheelPositions.frontLeftMeters,
            wheelPositions.frontRightMeters,
            wheelPositions.rearLeftMeters,
            wheelPositions.rearRightMeters);
  }

  public MecanumDriveOdometry(
      MecanumDriveKinematics kinematics,
      Rotation2d gyroAngle,
      MecanumDriveWheelPositions wheelPositions) {
    this(kinematics, gyroAngle, wheelPositions, new Pose2d());
  }

  public void resetPosition(
      Rotation2d gyroAngle, MecanumDriveWheelPositions wheelPositions, Pose2d pose) {
    this.pose = pose;
    previousAngle = gyroAngle;
    previousWheelPositions =
        new MecanumDriveWheelPositions(
            wheelPositions.frontLeftMeters,
            wheelPositions.frontRightMeters,
            wheelPositions.rearLeftMeters,
            wheelPositions.rearRightMeters);
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

  public Pose2d update(Rotation2d gyroAngle, MecanumDriveWheelPositions wheelPositions) {
    Twist2d twist = kinematics.toTwist2d(previousWheelPositions, wheelPositions);

    twist.dtheta = gyroAngle.minus(previousAngle).getRadians();

    pose = pose.exp(twist);

    previousAngle = gyroAngle;
    previousWheelPositions =
        new MecanumDriveWheelPositions(
            wheelPositions.frontLeftMeters,
            wheelPositions.frontRightMeters,
            wheelPositions.rearLeftMeters,
            wheelPositions.rearRightMeters);

    return pose;
  }
}

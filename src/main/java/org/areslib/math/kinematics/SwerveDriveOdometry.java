package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Twist2d;

/**
 * Class for swerve drive odometry. Odometry allows you to track the robot's position on the field
 * over a course of a match using readings from your swerve drive encoders and swerve azimuth
 * encoders.
 *
 * <p>This utilizes rigorous SwerveModulePosition tracking rather than velocity integration,
 * eliminating jitter or dropped intervals from control cycle stutters.
 */
public class SwerveDriveOdometry {
  private final SwerveDriveKinematics kinematics;
  private Pose2d pose;
  private Rotation2d previousAngle;
  private SwerveModulePosition[] previousModulePositions;

  /**
   * Constructs a SwerveDriveOdometry object.
   *
   * @param kinematics The swerve drive kinematics for your drivetrain.
   * @param gyroAngle The angle reported by the gyroscope.
   * @param modulePositions The current encoder readings of the swerve modules.
   * @param initialPose The starting position of the robot on the field.
   */
  public SwerveDriveOdometry(
      SwerveDriveKinematics kinematics,
      Rotation2d gyroAngle,
      SwerveModulePosition[] modulePositions,
      Pose2d initialPose) {
    this.kinematics = kinematics;
    pose = initialPose;
    previousAngle = gyroAngle;

    previousModulePositions = new SwerveModulePosition[modulePositions.length];
    for (int i = 0; i < modulePositions.length; i++) {
      previousModulePositions[i] =
          new SwerveModulePosition(modulePositions[i].distanceMeters, modulePositions[i].angle);
    }
  }

  public SwerveDriveOdometry(
      SwerveDriveKinematics kinematics,
      Rotation2d gyroAngle,
      SwerveModulePosition[] modulePositions) {
    this(kinematics, gyroAngle, modulePositions, new Pose2d());
  }

  /**
   * Resets the robot's position on the field.
   *
   * @param gyroAngle The angle reported by the gyroscope.
   * @param modulePositions The current encoder readings of the swerve modules.
   * @param pose The position on the field that your robot is at.
   */
  public void resetPosition(
      Rotation2d gyroAngle, SwerveModulePosition[] modulePositions, Pose2d pose) {
    this.pose = pose;
    previousAngle = gyroAngle;
    for (int i = 0; i < modulePositions.length; i++) {
      previousModulePositions[i] =
          new SwerveModulePosition(modulePositions[i].distanceMeters, modulePositions[i].angle);
    }
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

  /**
   * Retrieves the estimated pose of the robot.
   *
   * @return The estimated pose.
   */
  public Pose2d getPose() {
    return pose;
  }

  /**
   * Updates the robot's position on the field using forward kinematics and integration of the pose
   * over time.
   *
   * @param gyroAngle The current angle reported by the gyroscope.
   * @param modulePositions The current encoder readings of the swerve modules.
   * @return The new pose of the robot.
   */
  public Pose2d update(Rotation2d gyroAngle, SwerveModulePosition[] modulePositions) {
    if (modulePositions.length != previousModulePositions.length) {
      throw new IllegalArgumentException("Number of modules must remain constant.");
    }

    Twist2d twist = kinematics.toTwist2d(previousModulePositions, modulePositions);

    // WPILib exact odometry substitution: Gyro defines absolute dtheta!
    twist.dtheta = gyroAngle.minus(previousAngle).getRadians();

    // Exact exponential curve geometry mapping
    pose = pose.exp(twist);

    previousAngle = gyroAngle;
    for (int i = 0; i < modulePositions.length; i++) {
      previousModulePositions[i].distanceMeters = modulePositions[i].distanceMeters;
      previousModulePositions[i].angle = modulePositions[i].angle;
    }

    return pose;
  }
}

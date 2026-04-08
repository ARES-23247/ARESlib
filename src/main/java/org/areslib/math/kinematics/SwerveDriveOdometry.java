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
  private final SwerveDriveKinematics m_kinematics;
  private Pose2d m_pose;
  private Rotation2d m_previousAngle;
  private SwerveModulePosition[] m_previousModulePositions;

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
    m_kinematics = kinematics;
    m_pose = initialPose;
    m_previousAngle = gyroAngle;

    m_previousModulePositions = new SwerveModulePosition[modulePositions.length];
    for (int i = 0; i < modulePositions.length; i++) {
      m_previousModulePositions[i] =
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
    m_pose = pose;
    m_previousAngle = gyroAngle;
    for (int i = 0; i < modulePositions.length; i++) {
      m_previousModulePositions[i] =
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
    m_pose = pose;
  }

  /**
   * Retrieves the estimated pose of the robot.
   *
   * @return The estimated pose.
   */
  public Pose2d getPose() {
    return m_pose;
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
    if (modulePositions.length != m_previousModulePositions.length) {
      throw new IllegalArgumentException("Number of modules must remain constant.");
    }

    Twist2d twist = m_kinematics.toTwist2d(m_previousModulePositions, modulePositions);

    // WPILib exact odometry substitution: Gyro defines absolute dtheta!
    twist.dtheta = gyroAngle.minus(m_previousAngle).getRadians();

    // Exact exponential curve geometry mapping
    m_pose = m_pose.exp(twist);

    m_previousAngle = gyroAngle;
    for (int i = 0; i < modulePositions.length; i++) {
      m_previousModulePositions[i] =
          new SwerveModulePosition(modulePositions[i].distanceMeters, modulePositions[i].angle);
    }

    return m_pose;
  }
}

package org.areslib.math;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.areslib.math.kinematics.ChassisSpeeds;

/**
 * Advanced Shot-On-The-Move mathematical solver ingested from Team 254 (2024).
 *
 * <p>Calculates exact trajectory kinematics, solving the quadratic equation for time-of-flight
 * while applying gravity and lift compensation. Adapted from FRC to FTC game piece physics.
 *
 * <p>Students: This is used for aiming mechanisms that need to launch game pieces while the robot
 * is still moving. The math accounts for the robot's velocity so the game piece arrives at the
 * target despite the robot drifting.
 */
public class EliteShooterMath {

  /**
   * Data class for holding calculated shot parameters. Contains the exact angles and feedforwards
   * needed to hit the target while moving.
   */
  public static class EliteShooterSetpoint {
    /** The yaw angle (in radians) the robot should face to aim at the virtual target. */
    public double robotAimYawRadians;

    /** Angular velocity feedforward (rad/s) to add to the chassis rotation controller. */
    public double chassisAngularFeedforward;

    /** The pitch/hood angle (radians from horizontal) for the launcher. */
    public double hoodRadians;

    /** Hood angular velocity feedforward to compensate for robot motion. */
    public double hoodFeedforward;

    /** The adjusted launch speed (m/s) after gravity and lift compensation. */
    public double launchSpeedMetersPerSec;

    /** Whether the solution is physically valid (positive time-of-flight). */
    public boolean isValid;
  }

  /**
   * Mathematically solves the exact shot state needed to hit a 3D target given current robot speeds
   * and constraints.
   *
   * @param robotPose Current field-relative robot pose.
   * @param fieldRelativeSpeeds Current field-relative speeds of the chassis.
   * @param targetX The X coordinate of the target on the field (meters).
   * @param targetY The Y coordinate of the target on the field (meters).
   * @param targetZ The Z coordinate (height) of the target on the field (meters).
   * @param releaseHeightZ Height of the robot's shooter mechanism from the floor (meters).
   * @param nominalShotSpeedMetersPerSec Base shot velocity output limit.
   * @param gravity Gravity constant (typically 9.81, positive downward).
   * @param liftCoefficient Aerodynamic lift coefficient of the game piece (0 for no lift).
   * @return Computed EliteShooterSetpoint with exact angles and feedforwards.
   */
  public static EliteShooterSetpoint calculateShotOnTheMove(
      Pose2d robotPose,
      ChassisSpeeds fieldRelativeSpeeds,
      double targetX,
      double targetY,
      double targetZ,
      double releaseHeightZ,
      double nominalShotSpeedMetersPerSec,
      double gravity,
      double liftCoefficient) {

    EliteShooterSetpoint setpoint = new EliteShooterSetpoint();

    // Vector from robot to target
    double dx = targetX - robotPose.getTranslation().getX();
    double dy = targetY - robotPose.getTranslation().getY();
    double dz = targetZ - releaseHeightZ;

    double vShot = nominalShotSpeedMetersPerSec;
    double vx = fieldRelativeSpeeds.vxMetersPerSecond;
    double vy = fieldRelativeSpeeds.vyMetersPerSecond;

    // Solve quadratic equation for time-of-flight:
    // a = vx^2 + vy^2 - vShot^2
    // b = -2 * (dx * vx + dy * vy)
    // c = dx^2 + dy^2 + dz^2
    double a = vx * vx + vy * vy - vShot * vShot;

    if (Math.abs(a) < 1e-6) {
      // Adjust to avoid division by zero / non-quadratic states
      vShot = 1.01 * vShot;
      a = vx * vx + vy * vy - vShot * vShot;
    }

    double b = -2.0 * (dx * vx + dy * vy);
    double c = dx * dx + dy * dy + dz * dz;

    double discriminant = b * b - 4.0 * a * c;
    if (discriminant < 0.0) {
      discriminant = 0.0;
    }

    // Solve for time of flight
    double t = (-b - Math.sqrt(discriminant)) / (2.0 * a);

    if (t <= 0) {
      setpoint.isValid = false;
      return setpoint;
    }

    // Virtual shot vector: where the ball needs to go relative to robot
    double virtualShotX = (dx - vx * t) / t;
    double virtualShotY = (dy - vy * t) / t;

    Rotation2d virtualTargetRotation = new Rotation2d(Math.atan2(virtualShotY, virtualShotX));
    double xyVel = Math.sqrt(virtualShotX * virtualShotX + virtualShotY * virtualShotY);

    // Apply gravity and lift compensation
    double drop = 0.5 * t * t * gravity;
    drop += 0.5 * liftCoefficient * c;

    double pitchAngleRads = Math.atan2((dz - drop) / t, xyVel);
    double adjustedVShot = Math.sqrt((dz - drop) * (dz - drop) / (t * t) + xyVel * xyVel);

    // Compute Chassis Aim and Feedforward
    double distanceToTarget = Math.sqrt(dx * dx + dy * dy);

    double chassisAngularFF = (dy * vx - dx * vy) / (distanceToTarget * distanceToTarget);

    // Project robot velocity into the target frame for hood feed-forward
    Translation2d velocityVec = new Translation2d(vx, vy);
    double cosAngle = Math.cos(virtualTargetRotation.getRadians());
    double sinAngle = Math.sin(virtualTargetRotation.getRadians());
    double projectedX = velocityVec.getX() * cosAngle + velocityVec.getY() * sinAngle;

    double hoodFF = projectedX * -dz / (distanceToTarget * distanceToTarget + dz * dz);

    setpoint.robotAimYawRadians = virtualTargetRotation.getRadians();
    setpoint.chassisAngularFeedforward = chassisAngularFF;
    setpoint.hoodRadians = pitchAngleRads;
    setpoint.hoodFeedforward = hoodFF;
    setpoint.launchSpeedMetersPerSec = adjustedVShot;
    setpoint.isValid = true;

    return setpoint;
  }
}

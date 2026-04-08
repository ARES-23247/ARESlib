package org.areslib.math.kinematics;

import org.areslib.math.geometry.Rotation2d;

/** Represents the state of one swerve module. */
public class SwerveModuleState {
  public double speedMetersPerSecond;
  public Rotation2d angle;

  public SwerveModuleState() {
    this.speedMetersPerSecond = 0.0;
    this.angle = new Rotation2d();
  }

  public SwerveModuleState(double speedMetersPerSecond, Rotation2d angle) {
    this.speedMetersPerSecond = speedMetersPerSecond;
    this.angle = angle;
  }

  /**
   * Minimize the change in heading the desired swerve module state would require by potentially
   * reversing the direction the wheel spins.
   *
   * @param desiredState The desired state.
   * @param currentAngle The current module angle.
   * @return Optimized swerve module state.
   */
  public static SwerveModuleState optimize(
      SwerveModuleState desiredState, Rotation2d currentAngle) {
    double targetAngle = desiredState.angle.getRadians();
    double currentAngleRad = currentAngle.getRadians();

    double delta = targetAngle - currentAngleRad;

    // Wrap to [-pi, pi]
    delta = delta % (2.0 * Math.PI);
    if (delta > Math.PI) {
      delta -= 2.0 * Math.PI;
    } else if (delta < -Math.PI) {
      delta += 2.0 * Math.PI;
    }

    targetAngle = currentAngleRad + delta;

    double targetSpeed = desiredState.speedMetersPerSecond;

    if (delta > Math.PI / 2.0) {
      targetAngle -= Math.PI;
      targetSpeed = -targetSpeed;
    } else if (delta < -Math.PI / 2.0) {
      targetAngle += Math.PI;
      targetSpeed = -targetSpeed;
    }

    return new SwerveModuleState(targetSpeed, new Rotation2d(targetAngle));
  }
}

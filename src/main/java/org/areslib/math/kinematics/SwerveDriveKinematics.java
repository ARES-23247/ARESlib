package org.areslib.math.kinematics;

import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;

/**
 * Helper class that converts a chassis velocity (dx, dy, and dtheta components) into individual
 * module states and vice versa.
 */
public class SwerveDriveKinematics {
  private final Translation2d[] modules;
  private final double[][] inverseKinematics;
  private final double[][] forwardKinematics;

  public SwerveDriveKinematics(Translation2d... moduleTranslations) {
    if (moduleTranslations.length < 2) {
      throw new IllegalArgumentException("A swerve drive requires at least two modules");
    }
    modules = moduleTranslations;
    int numModules = modules.length;

    inverseKinematics = new double[numModules * 2][3];
    for (int i = 0; i < numModules; i++) {
      inverseKinematics[i * 2][0] = 1;
      inverseKinematics[i * 2][1] = 0;
      inverseKinematics[i * 2][2] = -modules[i].getY();
      inverseKinematics[i * 2 + 1][0] = 0;
      inverseKinematics[i * 2 + 1][1] = 1;
      inverseKinematics[i * 2 + 1][2] = modules[i].getX();
    }

    forwardKinematics = InverseMatrixHelper.pseudoInverse(inverseKinematics);
  }

  /**
   * Converts a chassis speed to array of swerve module states.
   *
   * @param chassisSpeeds The chassis speeds.
   * @return Array of swerve module states.
   */
  public SwerveModuleState[] toSwerveModuleStates(ChassisSpeeds chassisSpeeds) {
    SwerveModuleState[] states = new SwerveModuleState[modules.length];
    for (int i = 0; i < modules.length; i++) {
      double vx =
          chassisSpeeds.vxMetersPerSecond * inverseKinematics[i * 2][0]
              + chassisSpeeds.vyMetersPerSecond * inverseKinematics[i * 2][1]
              + chassisSpeeds.omegaRadiansPerSecond * inverseKinematics[i * 2][2];

      double vy =
          chassisSpeeds.vxMetersPerSecond * inverseKinematics[i * 2 + 1][0]
              + chassisSpeeds.vyMetersPerSecond * inverseKinematics[i * 2 + 1][1]
              + chassisSpeeds.omegaRadiansPerSecond * inverseKinematics[i * 2 + 1][2];

      states[i] = new SwerveModuleState(Math.hypot(vx, vy), new Rotation2d(vx, vy));
    }
    return states;
  }

  /**
   * Converts an array of swerve module states into a single chassis speed.
   *
   * @param moduleStates Array of swerve module states.
   * @return The chassis speed.
   */
  public ChassisSpeeds toChassisSpeeds(SwerveModuleState... moduleStates) {
    if (moduleStates.length != modules.length) {
      throw new IllegalArgumentException("Number of module states must match number of modules");
    }

    double vx = 0;
    double vy = 0;
    double omega = 0;

    for (int i = 0; i < modules.length; i++) {
      double moduleVx = moduleStates[i].speedMetersPerSecond * moduleStates[i].angle.getCos();
      double moduleVy = moduleStates[i].speedMetersPerSecond * moduleStates[i].angle.getSin();

      vx += forwardKinematics[0][i * 2] * moduleVx + forwardKinematics[0][i * 2 + 1] * moduleVy;
      vy += forwardKinematics[1][i * 2] * moduleVx + forwardKinematics[1][i * 2 + 1] * moduleVy;
      omega += forwardKinematics[2][i * 2] * moduleVx + forwardKinematics[2][i * 2 + 1] * moduleVy;
    }

    return new ChassisSpeeds(vx, vy, omega);
  }

  /**
   * Renormalizes the wheel speeds if any individual speed is above the specified maximum.
   *
   * @param moduleStates The array of module states.
   * @param attainableMaxSpeedMetersPerSecond The absolute max speed that a module can reach.
   */
  public static void desaturateWheelSpeeds(
      SwerveModuleState[] moduleStates, double attainableMaxSpeedMetersPerSecond) {
    double realMaxSpeed = 0.0;
    for (SwerveModuleState state : moduleStates) {
      realMaxSpeed = Math.max(realMaxSpeed, Math.abs(state.speedMetersPerSecond));
    }
    if (realMaxSpeed > attainableMaxSpeedMetersPerSecond) {
      for (SwerveModuleState state : moduleStates) {
        state.speedMetersPerSecond =
            state.speedMetersPerSecond / realMaxSpeed * attainableMaxSpeedMetersPerSecond;
      }
    }
  }

  /**
   * Converts an array of swerve module position deltas into a single Twist2d delta.
   *
   * @param start Module positions at the start of the interval.
   * @param end Module positions at the end of the interval.
   * @return The twist over the interval.
   */
  public org.areslib.math.geometry.Twist2d toTwist2d(
      SwerveModulePosition[] start, SwerveModulePosition[] end) {
    if (start.length != modules.length || end.length != modules.length) {
      throw new IllegalArgumentException("Number of module positions must match number of modules");
    }

    SwerveModuleState[] moduleDeltas = new SwerveModuleState[modules.length];
    for (int i = 0; i < modules.length; i++) {
      moduleDeltas[i] =
          new SwerveModuleState(end[i].distanceMeters - start[i].distanceMeters, end[i].angle);
    }

    ChassisSpeeds twistSpeeds = toChassisSpeeds(moduleDeltas);
    return new org.areslib.math.geometry.Twist2d(
        twistSpeeds.vxMetersPerSecond,
        twistSpeeds.vyMetersPerSecond,
        twistSpeeds.omegaRadiansPerSecond);
  }
}

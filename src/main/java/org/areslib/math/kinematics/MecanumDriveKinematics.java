package org.areslib.math.kinematics;

import org.areslib.math.geometry.Translation2d;

public class MecanumDriveKinematics {
  private final double[][] inverseKinematics;
  private final double[][] forwardKinematics;

  public MecanumDriveKinematics(
      Translation2d frontLeftWheelMeters,
      Translation2d frontRightWheelMeters,
      Translation2d rearLeftWheelMeters,
      Translation2d rearRightWheelMeters) {

    inverseKinematics = new double[4][3];
    inverseKinematics[0][0] = 1;
    inverseKinematics[0][1] = -1;
    inverseKinematics[0][2] = -(frontLeftWheelMeters.getX() + frontLeftWheelMeters.getY());
    inverseKinematics[1][0] = 1;
    inverseKinematics[1][1] = 1;
    inverseKinematics[1][2] = frontRightWheelMeters.getX() - frontRightWheelMeters.getY();
    inverseKinematics[2][0] = 1;
    inverseKinematics[2][1] = 1;
    inverseKinematics[2][2] = rearLeftWheelMeters.getX() - rearLeftWheelMeters.getY();
    inverseKinematics[3][0] = 1;
    inverseKinematics[3][1] = -1;
    inverseKinematics[3][2] = -(rearRightWheelMeters.getX() + rearRightWheelMeters.getY());

    forwardKinematics = InverseMatrixHelper.pseudoInverse(inverseKinematics);
  }

  public MecanumDriveWheelSpeeds toWheelSpeeds(ChassisSpeeds chassisSpeeds) {
    return new MecanumDriveWheelSpeeds(
        chassisSpeeds.vxMetersPerSecond * inverseKinematics[0][0]
            + chassisSpeeds.vyMetersPerSecond * inverseKinematics[0][1]
            + chassisSpeeds.omegaRadiansPerSecond * inverseKinematics[0][2],
        chassisSpeeds.vxMetersPerSecond * inverseKinematics[1][0]
            + chassisSpeeds.vyMetersPerSecond * inverseKinematics[1][1]
            + chassisSpeeds.omegaRadiansPerSecond * inverseKinematics[1][2],
        chassisSpeeds.vxMetersPerSecond * inverseKinematics[2][0]
            + chassisSpeeds.vyMetersPerSecond * inverseKinematics[2][1]
            + chassisSpeeds.omegaRadiansPerSecond * inverseKinematics[2][2],
        chassisSpeeds.vxMetersPerSecond * inverseKinematics[3][0]
            + chassisSpeeds.vyMetersPerSecond * inverseKinematics[3][1]
            + chassisSpeeds.omegaRadiansPerSecond * inverseKinematics[3][2]);
  }

  public ChassisSpeeds toChassisSpeeds(MecanumDriveWheelSpeeds wheelSpeeds) {
    double vx =
        forwardKinematics[0][0] * wheelSpeeds.frontLeftMetersPerSecond
            + forwardKinematics[0][1] * wheelSpeeds.frontRightMetersPerSecond
            + forwardKinematics[0][2] * wheelSpeeds.rearLeftMetersPerSecond
            + forwardKinematics[0][3] * wheelSpeeds.rearRightMetersPerSecond;

    double vy =
        forwardKinematics[1][0] * wheelSpeeds.frontLeftMetersPerSecond
            + forwardKinematics[1][1] * wheelSpeeds.frontRightMetersPerSecond
            + forwardKinematics[1][2] * wheelSpeeds.rearLeftMetersPerSecond
            + forwardKinematics[1][3] * wheelSpeeds.rearRightMetersPerSecond;

    double omega =
        forwardKinematics[2][0] * wheelSpeeds.frontLeftMetersPerSecond
            + forwardKinematics[2][1] * wheelSpeeds.frontRightMetersPerSecond
            + forwardKinematics[2][2] * wheelSpeeds.rearLeftMetersPerSecond
            + forwardKinematics[2][3] * wheelSpeeds.rearRightMetersPerSecond;

    return new ChassisSpeeds(vx, vy, omega);
  }

  /**
   * Converts an array of mecanum wheel position deltas into a single Twist2d delta.
   *
   * @param start Wheel positions at the start of the interval.
   * @param end Wheel positions at the end of the interval.
   * @return The twist over the interval.
   */
  public org.areslib.math.geometry.Twist2d toTwist2d(
      MecanumDriveWheelPositions start, MecanumDriveWheelPositions end) {
    MecanumDriveWheelSpeeds deltas =
        new MecanumDriveWheelSpeeds(
            end.frontLeftMeters - start.frontLeftMeters,
            end.frontRightMeters - start.frontRightMeters,
            end.rearLeftMeters - start.rearLeftMeters,
            end.rearRightMeters - start.rearRightMeters);
    ChassisSpeeds twistSpeeds = toChassisSpeeds(deltas);
    return new org.areslib.math.geometry.Twist2d(
        twistSpeeds.vxMetersPerSecond,
        twistSpeeds.vyMetersPerSecond,
        twistSpeeds.omegaRadiansPerSecond);
  }
}

package org.areslib.math.kinematics;

/**
 * Helper class that converts a chassis velocity (dx and dtheta components) into individual wheel
 * speeds for a differential drive (tank drive) robot.
 */
public class DifferentialDriveKinematics {
  /** The track width in meters representing the distance between the left and right wheels. */
  public final double trackWidthMeters;

  /**
   * Constructs a differential drive kinematics object.
   *
   * @param trackWidthMeters The track width of the drivetrain.
   */
  public DifferentialDriveKinematics(double trackWidthMeters) {
    this.trackWidthMeters = trackWidthMeters;
  }

  /**
   * Converts a chassis speed to individual wheel speeds.
   *
   * @param chassisSpeeds The chassis speeds.
   * @return The individual wheel speeds.
   */
  public DifferentialDriveWheelSpeeds toWheelSpeeds(ChassisSpeeds chassisSpeeds) {
    return new DifferentialDriveWheelSpeeds(
        chassisSpeeds.vxMetersPerSecond
            - trackWidthMeters / 2.0 * chassisSpeeds.omegaRadiansPerSecond,
        chassisSpeeds.vxMetersPerSecond
            + trackWidthMeters / 2.0 * chassisSpeeds.omegaRadiansPerSecond);
  }

  /**
   * Converts individual wheel speeds to a single chassis speed.
   *
   * @param wheelSpeeds The individual wheel speeds.
   * @return The chassis speed.
   */
  public ChassisSpeeds toChassisSpeeds(DifferentialDriveWheelSpeeds wheelSpeeds) {
    return new ChassisSpeeds(
        (wheelSpeeds.leftMetersPerSecond + wheelSpeeds.rightMetersPerSecond) / 2.0,
        0.0,
        (wheelSpeeds.rightMetersPerSecond - wheelSpeeds.leftMetersPerSecond) / trackWidthMeters);
  }

  /**
   * Converts a wheel position delta to a Twist2d.
   *
   * @param start The starting wheel positions.
   * @param end The ending wheel positions.
   * @return The twist over the interval.
   */
  public org.areslib.math.geometry.Twist2d toTwist2d(
      DifferentialDriveWheelPositions start, DifferentialDriveWheelPositions end) {

    DifferentialDriveWheelSpeeds wheelDeltas =
        new DifferentialDriveWheelSpeeds(
            end.leftMeters - start.leftMeters, end.rightMeters - start.rightMeters);

    ChassisSpeeds twistSpeeds = toChassisSpeeds(wheelDeltas);
    return new org.areslib.math.geometry.Twist2d(
        twistSpeeds.vxMetersPerSecond,
        twistSpeeds.vyMetersPerSecond,
        twistSpeeds.omegaRadiansPerSecond);
  }
}

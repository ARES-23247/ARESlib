package org.areslib.math.kinematics;

/**
 * MecanumDriveWheelSpeeds standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * MecanumDriveWheelSpeeds}. Extracted and compiled as part of the ARESLib2 Code Audit for missing
 * documentation coverage.
 */
public class MecanumDriveWheelSpeeds {
  public double frontLeftMetersPerSecond;
  public double frontRightMetersPerSecond;
  public double rearLeftMetersPerSecond;
  public double rearRightMetersPerSecond;

  public MecanumDriveWheelSpeeds() {}

  public MecanumDriveWheelSpeeds(
      double frontLeftMetersPerSecond,
      double frontRightMetersPerSecond,
      double rearLeftMetersPerSecond,
      double rearRightMetersPerSecond) {
    this.frontLeftMetersPerSecond = frontLeftMetersPerSecond;
    this.frontRightMetersPerSecond = frontRightMetersPerSecond;
    this.rearLeftMetersPerSecond = rearLeftMetersPerSecond;
    this.rearRightMetersPerSecond = rearRightMetersPerSecond;
  }
}

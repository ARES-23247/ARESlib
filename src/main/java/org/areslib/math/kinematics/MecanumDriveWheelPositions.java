package org.areslib.math.kinematics;

/**
 * MecanumDriveWheelPositions standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * MecanumDriveWheelPositions}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public class MecanumDriveWheelPositions {
  public double frontLeftMeters;
  public double frontRightMeters;
  public double rearLeftMeters;
  public double rearRightMeters;

  public MecanumDriveWheelPositions() {}

  public MecanumDriveWheelPositions(
      double frontLeftMeters,
      double frontRightMeters,
      double rearLeftMeters,
      double rearRightMeters) {
    this.frontLeftMeters = frontLeftMeters;
    this.frontRightMeters = frontRightMeters;
    this.rearLeftMeters = rearLeftMeters;
    this.rearRightMeters = rearRightMeters;
  }
}

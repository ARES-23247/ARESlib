package org.areslib.math.kinematics;

/**
 * DifferentialDriveWheelSpeeds standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * DifferentialDriveWheelSpeeds}. Extracted and compiled as part of the ARESLib2 Code Audit for
 * missing documentation coverage.
 */
public class DifferentialDriveWheelSpeeds {
  public double leftMetersPerSecond;
  public double rightMetersPerSecond;

  public DifferentialDriveWheelSpeeds() {}

  public DifferentialDriveWheelSpeeds(double leftMetersPerSecond, double rightMetersPerSecond) {
    this.leftMetersPerSecond = leftMetersPerSecond;
    this.rightMetersPerSecond = rightMetersPerSecond;
  }
}

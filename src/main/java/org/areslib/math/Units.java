package org.areslib.math;

/**
 * Utility class for common unit conversions used in FTC robotics.
 *
 * <p>FTC hardware (GoBilda, REV) reports values in inches, degrees, and RPM, while ARESLib's math
 * stack uses meters, radians, and rad/s exclusively. This class bridges the gap and eliminates
 * hand-rolled conversion bugs.
 */
public final class Units {
  private Units() {
    throw new AssertionError("Utility class");
  }

  // ── Length ──────────────────────────────────────────────────────────────────

  private static final double METERS_PER_INCH = 0.0254;
  private static final double METERS_PER_FOOT = 0.3048;
  private static final double METERS_PER_MILLIMETER = 0.001;

  /**
   * Converts inches to meters.
   *
   * @param inches The inches value.
   * @return The meters value.
   */
  public static double inchesToMeters(double inches) {
    return inches * METERS_PER_INCH;
  }

  /**
   * Converts meters to inches.
   *
   * @param meters The meters value.
   * @return The inches value.
   */
  public static double metersToInches(double meters) {
    return meters / METERS_PER_INCH;
  }

  /**
   * Converts feet to meters.
   *
   * @param feet The feet value.
   * @return The meters value.
   */
  public static double feetToMeters(double feet) {
    return feet * METERS_PER_FOOT;
  }

  /**
   * Converts meters to feet.
   *
   * @param meters The meters value.
   * @return The feet value.
   */
  public static double metersToFeet(double meters) {
    return meters / METERS_PER_FOOT;
  }

  /**
   * Converts millimeters to meters.
   *
   * @param mm The millimeters value.
   * @return The meters value.
   */
  public static double millimetersToMeters(double mm) {
    return mm * METERS_PER_MILLIMETER;
  }

  /**
   * Converts meters to millimeters.
   *
   * @param meters The meters value.
   * @return The millimeters value.
   */
  public static double metersToMillimeters(double meters) {
    return meters / METERS_PER_MILLIMETER;
  }

  // ── Angle ──────────────────────────────────────────────────────────────────

  /**
   * Converts degrees to radians.
   *
   * @param degrees The degrees value.
   * @return The radians value.
   */
  public static double degreesToRadians(double degrees) {
    return Math.toRadians(degrees);
  }

  /**
   * Converts radians to degrees.
   *
   * @param radians The radians value.
   * @return The degrees value.
   */
  public static double radiansToDegrees(double radians) {
    return Math.toDegrees(radians);
  }

  /**
   * Converts rotations (full turns) to radians.
   *
   * @param rotations The rotations value.
   * @return The radians value.
   */
  public static double rotationsToRadians(double rotations) {
    return rotations * 2.0 * Math.PI;
  }

  /**
   * Converts radians to rotations (full turns).
   *
   * @param radians The radians value.
   * @return The rotations value.
   */
  public static double radiansToRotations(double radians) {
    return radians / (2.0 * Math.PI);
  }

  // ── Angular Velocity ───────────────────────────────────────────────────────

  /**
   * Converts RPM (revolutions per minute) to radians per second.
   *
   * @param rpm The rpm value.
   * @return The radians per second value.
   */
  public static double rpmToRadPerSec(double rpm) {
    return rpm * (2.0 * Math.PI) / 60.0;
  }

  /**
   * Converts radians per second to RPM.
   *
   * @param radPerSec The radians per second value.
   * @return The rpm value.
   */
  public static double radPerSecToRPM(double radPerSec) {
    return radPerSec * 60.0 / (2.0 * Math.PI);
  }
}

package org.areslib.core;

/**
 * Utility functions for coordinate conversions, interpolation, and geometry scaling used within the
 * ARESLib framework.
 */
public class CoordinateUtil {

  /**
   * Converts inches to meters.
   *
   * @param inches The distance in inches
   * @return The distance in meters
   */
  public static double inchesToMeters(double inches) {
    return inches * 0.0254;
  }

  /**
   * Converts meters to inches.
   *
   * @param meters The distance in meters
   * @return The distance in inches
   */
  public static double metersToInches(double meters) {
    return meters / 0.0254;
  }

  /**
   * Converts millimeters to meters.
   *
   * @param mm The distance in millimeters
   * @return The distance in meters
   */
  public static double mmToMeters(double mm) {
    return mm / 1000.0;
  }

  /**
   * Linearly interpolates between a and b by percentage f.
   *
   * @param a The start value
   * @param b The end value
   * @param f The parameter, 0-1
   * @return The interpolated value
   */
  public static double lerp(double a, double b, double f) {
    return a + f * (b - a);
  }

  /**
   * Linearly interpolates between two angles in radians across the shortest path.
   *
   * @param a The start angle in radians
   * @param b The end angle in radians
   * @param f The parameter, 0-1
   * @return The interpolated angle in radians
   */
  public static double shortestAngleLerp(double a, double b, double f) {
    double diff = b - a;
    while (diff > Math.PI) diff -= 2 * Math.PI;
    while (diff < -Math.PI) diff += 2 * Math.PI;
    return a + diff * f;
  }

  /**
   * Retrieves the Kalman gain multiplier based on vision confidence.
   *
   * @param confidence Camera confidence metric
   * @return Kalman gain scaler
   */
  public static double computeVisionKalmanGain(double confidence) {
    return 0.1 * confidence; // simplified trust model
  }
}

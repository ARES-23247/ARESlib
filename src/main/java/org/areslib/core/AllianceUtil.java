package org.areslib.core;

/**
 * Utility for detecting and handling alliance color in FTC matches.
 *
 * <p>In FTC, the alliance color is typically determined by the OpMode name convention or by reading
 * from the Driver Station. This utility provides a centralized way to track alliance state for
 * coordinate flipping, autonomous mirroring, and LED color selection.
 */
public class AllianceUtil {

  /** The two possible alliances in FTC competition. */
  public enum Alliance {
    RED,
    BLUE
  }

  private static Alliance currentAlliance = Alliance.RED;

  /**
   * Sets the current alliance. Should be called during initialization based on the selected OpMode.
   *
   * @param alliance The alliance color for this match.
   */
  public static void setAlliance(Alliance alliance) {
    currentAlliance = alliance;
  }

  /**
   * Returns the current alliance.
   *
   * @return The alliance color.
   */
  public static Alliance getAlliance() {
    return currentAlliance;
  }

  /**
   * Returns true if the current alliance is Red.
   *
   * @return True if Red alliance.
   */
  public static boolean isRed() {
    return currentAlliance == Alliance.RED;
  }

  /**
   * Returns true if the current alliance is Blue.
   *
   * @return True if Blue alliance.
   */
  public static boolean isBlue() {
    return currentAlliance == Alliance.BLUE;
  }

  /**
   * Flips an X coordinate for coordinate mirroring between alliances. In FTC, the field is
   * symmetric about the center, so Red-side coordinates need to be negated when running Blue-side
   * autonomous routines.
   *
   * @param x The X coordinate in Red-alliance frame.
   * @return The equivalent X coordinate for the current alliance.
   */
  public static double flipX(double x) {
    return isRed() ? x : -x;
  }

  /**
   * Flips a heading angle for the opposite alliance side.
   *
   * @param headingRadians The heading in Red-alliance frame (radians).
   * @return The equivalent heading for the current alliance.
   */
  public static double flipHeading(double headingRadians) {
    return isRed() ? headingRadians : Math.PI - headingRadians;
  }

  /** Resets the alliance to the default (Red). */
  public static void reset() {
    currentAlliance = Alliance.RED;
  }
}

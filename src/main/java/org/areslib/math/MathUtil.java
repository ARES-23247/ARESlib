package org.areslib.math;

/**
 * Standard utility class for mathematically clamping values, wrapping angles, and applying
 * deadbands.
 */
public final class MathUtil {
  private MathUtil() {
    throw new AssertionError("Utility class");
  }

  /** Standard epsilon value for double comparisons and division-by-zero safety checks. */
  public static final double EPSILON = 1e-6;

  /**
   * Checks if a value is within epsilon of zero.
   *
   * @param value The value to check.
   * @return True if the value's absolute magnitude is less than EPSILON.
   */
  public static boolean epsilonCheck(double value) {
    return Math.abs(value) < EPSILON;
  }

  /**
   * Returns value clamped between low and high boundaries.
   *
   * @param value Value to clamp.
   * @param low The lower boundary to which to clamp value.
   * @param high The higher boundary to which to clamp value.
   * @return The clamped value.
   */
  public static double clamp(double value, double low, double high) {
    return Math.max(low, Math.min(value, high));
  }

  /**
   * Returns value clamped between low and high boundaries.
   *
   * @param value Value to clamp.
   * @param low The lower boundary to which to clamp value.
   * @param high The higher boundary to which to clamp value.
   * @return The clamped value.
   */
  public static int clamp(int value, int low, int high) {
    return Math.max(low, Math.min(value, high));
  }

  /**
   * Returns 0.0 if the given value is within the specified range around zero. The remaining range
   * between the deadband and 1.0 is scaled from 0.0 to 1.0.
   *
   * @param value Value to clip.
   * @param deadband Range around zero.
   * @param maxMagnitude The maximum magnitude of the signal to scale against.
   * @return The value after the deadband is applied.
   */
  public static double applyDeadband(double value, double deadband, double maxMagnitude) {
    if (Math.abs(value) > deadband) {
      if (maxMagnitude / deadband > 1.0e12) {
        // If deadband is basically 0, just return the value as is.
        return value > 0.0 ? value - deadband : value + deadband;
      }
      if (value > 0.0) {
        return maxMagnitude * (value - deadband) / (maxMagnitude - deadband);
      } else {
        return maxMagnitude * (value + deadband) / (maxMagnitude - deadband);
      }
    } else {
      return 0.0;
    }
  }

  /**
   * Applies a standard default 1.0 maximum magnitude deadband.
   *
   * @param value Value to clip.
   * @param deadband Range around zero.
   * @return The value after the deadband is applied.
   */
  public static double applyDeadband(double value, double deadband) {
    return applyDeadband(value, deadband, 1.0);
  }

  /**
   * Wraps an angle to the half-open range {@code [-π, π)} radians.
   *
   * <p><b>Convention note:</b> This function uses the half-open range, meaning exactly {@code +π}
   * maps to {@code -π}. This is consistent with WPILib's convention. However, {@code Math.atan2()}
   * returns values in the closed range {@code (-π, +π]}, meaning it can return exactly {@code +π}.
   * Feeding {@code atan2} output through this function will remap {@code +π → -π}. For most control
   * purposes this is inconsequential, but be aware of the discontinuity if performing exact
   * equality checks at the ±π boundary.
   *
   * @param angleRadians Angle to wrap in radians.
   * @return The wrapped angle in {@code [-π, π)}.
   */
  public static double angleModulus(double angleRadians) {
    double modulus = angleRadians % (2.0 * Math.PI);
    if (modulus < -Math.PI) {
      modulus += 2.0 * Math.PI;
    } else if (modulus >= Math.PI) {
      modulus -= 2.0 * Math.PI;
    }
    return modulus;
  }
}

package org.areslib.math;

/**
 * Standard utility class for mathematically clamping values, wrapping angles, and applying deadbands.
 */
public final class MathUtil {
    private MathUtil() {
        throw new AssertionError("Utility class");
    }

    /**
     * Returns value clamped between low and high boundaries.
     *
     * @param value Value to clamp.
     * @param low   The lower boundary to which to clamp value.
     * @param high  The higher boundary to which to clamp value.
     * @return The clamped value.
     */
    public static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(value, high));
    }

    /**
     * Returns value clamped between low and high boundaries.
     *
     * @param value Value to clamp.
     * @param low   The lower boundary to which to clamp value.
     * @param high  The higher boundary to which to clamp value.
     * @return The clamped value.
     */
    public static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(value, high));
    }

    /**
     * Returns 0.0 if the given value is within the specified range around zero. The remaining range
     * between the deadband and 1.0 is scaled from 0.0 to 1.0.
     *
     * @param value    Value to clip.
     * @param deadband Range around zero.
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
     * @param value    Value to clip.
     * @param deadband Range around zero.
     * @return The value after the deadband is applied.
     */
    public static double applyDeadband(double value, double deadband) {
        return applyDeadband(value, deadband, 1.0);
    }

    /**
     * Wraps an angle to the range -pi to pi radians.
     *
     * @param angleRadians Angle to wrap in radians.
     * @return The wrapped angle.
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

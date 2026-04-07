package org.areslib.math;

/**
 * Utility class for common unit conversions used in FTC robotics.
 * <p>
 * FTC hardware (GoBilda, REV) reports values in inches, degrees, and RPM, while
 * ARESLib's math stack uses meters, radians, and rad/s exclusively. This class
 * bridges the gap and eliminates hand-rolled conversion bugs.
 */
public final class Units {
    private Units() {
        throw new AssertionError("Utility class");
    }

    // ── Length ──────────────────────────────────────────────────────────────────

    private static final double kMetersPerInch = 0.0254;
    private static final double kMetersPerFoot = 0.3048;
    private static final double kMetersPerMillimeter = 0.001;

    /** Converts inches to meters. */
    public static double inchesToMeters(double inches) {
        return inches * kMetersPerInch;
    }

    /** Converts meters to inches. */
    public static double metersToInches(double meters) {
        return meters / kMetersPerInch;
    }

    /** Converts feet to meters. */
    public static double feetToMeters(double feet) {
        return feet * kMetersPerFoot;
    }

    /** Converts meters to feet. */
    public static double metersToFeet(double meters) {
        return meters / kMetersPerFoot;
    }

    /** Converts millimeters to meters. */
    public static double millimetersToMeters(double mm) {
        return mm * kMetersPerMillimeter;
    }

    /** Converts meters to millimeters. */
    public static double metersToMillimeters(double meters) {
        return meters / kMetersPerMillimeter;
    }

    // ── Angle ──────────────────────────────────────────────────────────────────

    /** Converts degrees to radians. */
    public static double degreesToRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    /** Converts radians to degrees. */
    public static double radiansToDegrees(double radians) {
        return Math.toDegrees(radians);
    }

    /** Converts rotations (full turns) to radians. */
    public static double rotationsToRadians(double rotations) {
        return rotations * 2.0 * Math.PI;
    }

    /** Converts radians to rotations (full turns). */
    public static double radiansToRotations(double radians) {
        return radians / (2.0 * Math.PI);
    }

    // ── Angular Velocity ───────────────────────────────────────────────────────

    /** Converts RPM (revolutions per minute) to radians per second. */
    public static double rpmToRadPerSec(double rpm) {
        return rpm * (2.0 * Math.PI) / 60.0;
    }

    /** Converts radians per second to RPM. */
    public static double radPerSecToRPM(double radPerSec) {
        return radPerSec * 60.0 / (2.0 * Math.PI);
    }
}

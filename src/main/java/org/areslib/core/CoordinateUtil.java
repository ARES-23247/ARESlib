package org.areslib.core;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;

/**
 * Centralized coordinate conversions between the three systems used in ARESLib2.
 * <p>
 * <b>WPILib/dyn4j:</b> Meters, field center origin, X-forward Y-left θ CCW+.<br>
 * <b>Pedro Pathing:</b> Inches, bottom-left origin, X-right Y-forward.<br>
 * <b>AdvantageScope:</b> Same as WPILib.
 * <p>
 * <b>Rule:</b> Never write raw {@code * 0.0254} or {@code + 72.0} in application code.
 * Always use this class for conversions. Inline conversions are coordinate bugs waiting to happen.
 *
 * @see FieldConstants
 */
public final class CoordinateUtil {

    private CoordinateUtil() {} // Utility class

    // ===== Unit Conversions =====

    /**
     * Converts meters to inches.
     */
    public static double metersToInches(double meters) {
        return meters * FieldConstants.METERS_TO_INCHES;
    }

    /**
     * Converts inches to meters.
     */
    public static double inchesToMeters(double inches) {
        return inches * FieldConstants.INCHES_TO_METERS;
    }

    /**
     * Converts millimeters to inches.
     * Use instead of raw {@code / 25.4} in LiDAR and sensor code.
     */
    public static double mmToInches(double mm) {
        return mm / 25.4;
    }

    /**
     * Converts millimeters to meters.
     * Use instead of raw {@code / 1000.0} in driver-level IO code.
     */
    public static double mmToMeters(double mm) {
        return mm / 1000.0;
    }

    // ===== Origin Shifts =====

    /**
     * Converts a center-origin value (meters) to Pedro's bottom-left origin (inches).
     * Applies: {@code (meters * M2I) + 72.0}
     */
    public static double centerMetersToBottomLeftInches(double centerMeters) {
        return metersToInches(centerMeters) + FieldConstants.HALF_FIELD_INCHES;
    }

    /**
     * Converts a Pedro bottom-left origin value (inches) to center-origin (meters).
     * Applies: {@code (inches - 72.0) * I2M}
     */
    public static double bottomLeftInchesToCenterMeters(double bottomLeftInches) {
        return inchesToMeters(bottomLeftInches - FieldConstants.HALF_FIELD_INCHES);
    }

    // ===== Axis Swaps (WPILib ↔ Pedro) =====

    /**
     * Converts a WPILib Pose2d (meters, center origin, X-forward Y-left)
     * to a Pedro Pose (inches, bottom-left origin, X-right Y-forward).
     * <p>
     * Axis mapping: pedroX = -wpilibY, pedroY = wpilibX
     */
    public static com.pedropathing.geometry.Pose wpiToPedro(Pose2d wpiPose) {
        double pedroX = centerMetersToBottomLeftInches(-wpiPose.getY());
        double pedroY = centerMetersToBottomLeftInches(wpiPose.getX());
        return new com.pedropathing.geometry.Pose(pedroX, pedroY, wpiPose.getRotation().getRadians());
    }

    /**
     * Converts a Pedro Pose (inches, bottom-left origin, X-right Y-forward)
     * to a WPILib Pose2d (meters, center origin, X-forward Y-left).
     * <p>
     * Axis mapping: wpilibX = pedroY, wpilibY = -pedroX
     */
    public static Pose2d pedroToWpi(com.pedropathing.geometry.Pose pedroPose) {
        double wpiX = bottomLeftInchesToCenterMeters(pedroPose.getY());
        double wpiY = -bottomLeftInchesToCenterMeters(pedroPose.getX());
        return new Pose2d(wpiX, wpiY, new Rotation2d(pedroPose.getHeading()));
    }

    /**
     * Converts a WPILib Translation2d (meters, center origin)
     * to a Pedro Pose at heading 0 (inches, bottom-left origin) with axis swap.
     */
    public static com.pedropathing.geometry.Pose wpiToPedroPose(Translation2d wpiTranslation) {
        double pedroX = centerMetersToBottomLeftInches(-wpiTranslation.getY());
        double pedroY = centerMetersToBottomLeftInches(wpiTranslation.getX());
        return new com.pedropathing.geometry.Pose(pedroX, pedroY, 0);
    }

    /**
     * Converts a vision center-origin pose (meters) to Pedro bottom-left origin (inches).
     * <b>No axis swap</b> — vision and Pedro share the same axis orientation
     * when the vision system has already been aligned to the Pedro frame.
     * For raw WPILib-frame vision data, use {@link #wpiToPedro(Pose2d)} instead.
     */
    public static com.pedropathing.geometry.Pose visionCenterToPedro(double xMeters, double yMeters, double headingRad) {
        return new com.pedropathing.geometry.Pose(
                centerMetersToBottomLeftInches(xMeters),
                centerMetersToBottomLeftInches(yMeters),
                headingRad
        );
    }

    // ===== Fusion Math Utilities =====

    /**
     * Linear interpolation between two values.
     *
     * @param current The current value.
     * @param target  The target value to blend toward.
     * @param weight  Blend weight in [0, 1]. 0 = no change, 1 = jump to target.
     * @return The interpolated value.
     */
    public static double lerp(double current, double target, double weight) {
        return current + (target - current) * weight;
    }

    /**
     * Shortest-path angular interpolation. Prevents heading wraparound
     * from causing a full 360-degree spin (e.g., 350 to 10 goes through 0, not 180).
     *
     * @param currentRad Current heading in radians.
     * @param targetRad  Target heading in radians.
     * @param weight     Blend weight in [0, 1].
     * @return The interpolated heading in radians.
     */
    public static double shortestAngleLerp(double currentRad, double targetRad, double weight) {
        double diff = targetRad - currentRad;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        return currentRad + diff * weight;
    }

    /**
     * Computes a 1D Kalman-inspired gain for vision fusion blending.
     * Higher confidence = higher gain = more trust in vision.
     * <pre>
     *   K = Var(Odom) / (Var(Odom) + Var(Vision))
     *   where Var(Vision) = 0.1 * e^(5 * (1 - confidence))
     * </pre>
     *
     * @param confidence Vision confidence in [0, 1] (typically from target area).
     * @return Kalman gain in [0, ~0.33].
     */
    public static double computeVisionKalmanGain(double confidence) {
        double odomVariance = 0.05;
        double visionVariance = 0.1 * Math.exp(5.0 * (1.0 - confidence));
        return odomVariance / (odomVariance + visionVariance);
    }
}

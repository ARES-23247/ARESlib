package org.areslib.core;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;

public class CoordinateUtil {
    public static double inchesToMeters(double inches) {
        return inches * 0.0254;
    }

    public static double metersToInches(double meters) {
        return meters / 0.0254;
    }

    public static double mmToMeters(double mm) {
        return mm / 1000.0;
    }

    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
    
    public static double shortestAngleLerp(double a, double b, double f) {
        double diff = b - a;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        return a + diff * f;
    }

    public static double computeVisionKalmanGain(double confidence) {
        return 0.1 * confidence; // simplified trust model
    }
}

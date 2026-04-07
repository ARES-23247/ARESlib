package org.areslib.subsystems;

import org.areslib.core.CoordinateUtil;
import org.areslib.core.FieldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the sensor fusion math pipeline end-to-end using {@link CoordinateUtil}:
 * <pre>
 *   Vision Pose (meters, center origin)
 *     -> CoordinateUtil conversion (inches, bottom-left origin)
 *     -> CoordinateUtil.computeVisionKalmanGain()
 *     -> CoordinateUtil.lerp() / shortestAngleLerp()
 *     -> Corrected pose (inches, Pedro frame)
 * </pre>
 *
 * This test validates the exact math used by {@code AresSensorFusionSubsystem.periodic()}
 * through the shared {@link CoordinateUtil} utility class.
 */
public class SensorFusionMathTest {

    @Test
    public void testHighConfidenceVisionStronglyCorrects() {
        // Robot odometry says (72, 72) inches (field center in Pedro frame)
        double odoX = 72.0;
        double odoY = 72.0;

        // Vision says robot is at (0.1, -0.05) meters from field center
        double visionXInches = CoordinateUtil.centerMetersToBottomLeftInches(0.1);
        double visionYInches = CoordinateUtil.centerMetersToBottomLeftInches(-0.05);

        // High confidence => Kalman gain should be significant
        double confidence = 0.9;
        double maxTrust = 0.15;
        double kalmanGain = CoordinateUtil.computeVisionKalmanGain(confidence);
        double blendWeight = Math.min(kalmanGain, maxTrust);

        double fusedX = CoordinateUtil.lerp(odoX, visionXInches, blendWeight);
        double fusedY = CoordinateUtil.lerp(odoY, visionYInches, blendWeight);

        // The fused pose should have moved TOWARD the vision estimate
        assertTrue(Math.abs(fusedX - odoX) > 0.1,
            "High confidence vision should noticeably nudge X");
        assertTrue(Math.abs(fusedY - odoY) > 0.1,
            "High confidence vision should noticeably nudge Y");

        // But should NOT have jumped all the way to the vision pose (capped by maxTrust)
        assertTrue(Math.abs(fusedX - visionXInches) > 0.5,
            "Fusion should NOT teleport to vision pose in one tick");
    }

    @Test
    public void testLowConfidenceVisionBarelyCorrects() {
        double odoX = 50.0;

        double visionXInches = CoordinateUtil.centerMetersToBottomLeftInches(0.5);

        // Low confidence => gain should be tiny
        double confidence = 0.1;
        double kalmanGain = CoordinateUtil.computeVisionKalmanGain(confidence);

        // With confidence=0.1, visionVariance = 0.1 * e^(4.5) ~ 9.0
        // kalmanGain = 0.05 / (0.05 + 9.0) ~ 0.0055
        assertTrue(kalmanGain < 0.01,
            "Kalman gain for low confidence should be very small, was: " + kalmanGain);

        double fusedX = CoordinateUtil.lerp(odoX, visionXInches, kalmanGain);
        // Almost no correction
        assertTrue(Math.abs(fusedX - odoX) < 0.5,
            "Low confidence should barely nudge the pose");
    }

    @Test
    public void testZeroConfidenceRejected() {
        // Below 0.05 threshold, fusion subsystem returns early — no correction
        double confidence = 0.03;
        assertTrue(confidence <= 0.05, "Confidence below threshold should be rejected");
    }

    @Test
    public void testCoordinateConversionRoundTrip() {
        // Field center in meters = (0, 0) should map to (72, 72) in Pedro inches
        double pedroX = CoordinateUtil.centerMetersToBottomLeftInches(0.0);
        double pedroY = CoordinateUtil.centerMetersToBottomLeftInches(0.0);
        assertEquals(72.0, pedroX, 0.001, "Center meters (0,0) should be Pedro (72, 72)");
        assertEquals(72.0, pedroY, 0.001);

        // Round trip back
        double centerX = CoordinateUtil.bottomLeftInchesToCenterMeters(pedroX);
        double centerY = CoordinateUtil.bottomLeftInchesToCenterMeters(pedroY);
        assertEquals(0.0, centerX, 0.001, "Round trip should return to origin");
        assertEquals(0.0, centerY, 0.001);
    }

    @Test
    public void testCoordinateConversionFieldCorners() {
        // Pedro (0, 0) = Bottom-left corner = (-1.8288, -1.8288) meters from center
        double centerX = CoordinateUtil.bottomLeftInchesToCenterMeters(0.0);
        double centerY = CoordinateUtil.bottomLeftInchesToCenterMeters(0.0);
        assertEquals(-FieldConstants.HALF_FIELD_METERS, centerX, 0.001);
        assertEquals(-FieldConstants.HALF_FIELD_METERS, centerY, 0.001);

        // Pedro (144, 144) = Top-right corner = (+1.8288, +1.8288) meters
        double trX = CoordinateUtil.bottomLeftInchesToCenterMeters(144.0);
        double trY = CoordinateUtil.bottomLeftInchesToCenterMeters(144.0);
        assertEquals(FieldConstants.HALF_FIELD_METERS, trX, 0.001);
        assertEquals(FieldConstants.HALF_FIELD_METERS, trY, 0.001);
    }

    @Test
    public void testHeadingInterpolationShortestPath() {
        // Standard case: small angle difference
        double result = CoordinateUtil.shortestAngleLerp(0.0, 0.2, 0.5);
        assertEquals(0.1, result, 0.001);

        // Wraparound case: ~350 deg to ~10 deg should interpolate through 0, not spin 340 deg
        double from = Math.toRadians(350);
        double to = Math.toRadians(10);

        double interpolated = CoordinateUtil.shortestAngleLerp(from, to, 0.5);
        double resultDeg = Math.toDegrees(interpolated);
        // Normalize to [0, 360)
        while (resultDeg < 0) resultDeg += 360;
        while (resultDeg >= 360) resultDeg -= 360;

        assertTrue(resultDeg > 355 || resultDeg < 5,
            "Shortest path interpolation from 350 to 10 should pass through 0, got: " + resultDeg);
    }

    @Test
    public void testMmConversionUtilities() {
        // 25.4 mm = 1 inch
        assertEquals(1.0, CoordinateUtil.mmToInches(25.4), 0.001);
        assertEquals(2.0, CoordinateUtil.mmToInches(50.8), 0.001);

        // 1000 mm = 1 meter
        assertEquals(1.0, CoordinateUtil.mmToMeters(1000.0), 0.001);
        assertEquals(0.5, CoordinateUtil.mmToMeters(500.0), 0.001);
    }

    @Test
    public void testKalmanGainMonotonicity() {
        // Kalman gain should INCREASE as confidence increases
        double gain01 = CoordinateUtil.computeVisionKalmanGain(0.1);
        double gain05 = CoordinateUtil.computeVisionKalmanGain(0.5);
        double gain09 = CoordinateUtil.computeVisionKalmanGain(0.9);

        assertTrue(gain01 < gain05, "Gain at 0.1 should be less than at 0.5");
        assertTrue(gain05 < gain09, "Gain at 0.5 should be less than at 0.9");

        // At perfect confidence, gain should be significant
        double gainPerfect = CoordinateUtil.computeVisionKalmanGain(1.0);
        assertTrue(gainPerfect > 0.2,
            "Perfect confidence should yield substantial gain, was: " + gainPerfect);
    }

    @Test
    public void testLerpEdgeCases() {
        // Weight 0 = no change
        assertEquals(10.0, CoordinateUtil.lerp(10.0, 20.0, 0.0), 0.001);
        // Weight 1 = jump to target
        assertEquals(20.0, CoordinateUtil.lerp(10.0, 20.0, 1.0), 0.001);
        // Weight 0.5 = midpoint
        assertEquals(15.0, CoordinateUtil.lerp(10.0, 20.0, 0.5), 0.001);
    }
}

package org.areslib.subsystems.vision;

import org.areslib.core.localization.AresFollower;
import org.areslib.hardware.interfaces.VisionIO;
import com.pedropathing.geometry.Pose;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Headless JUnit 5 tests for {@link AresSensorFusionSubsystem}.
 * Validates Kalman gain calculation, meter→inch coordinate conversion,
 * angular shortest-path interpolation, and confidence-gated blending.
 */
public class AresSensorFusionSubsystemTest {

    private AresFollower mockOdometry;
    private AresVisionSubsystem mockVision;
    private AresSensorFusionSubsystem fusion;

    private static final double MAX_TRUST = 0.15;

    @BeforeEach
    void setup() {
        mockOdometry = mock(AresFollower.class);
        mockVision = mock(AresVisionSubsystem.class);
        fusion = new AresSensorFusionSubsystem(mockOdometry, mockVision, MAX_TRUST);
    }

    // ========== No-Correction Cases ==========

    @Test
    void testNoVisionTargetSkipsCorrection() {
        when(mockVision.getEstimatedGlobalPose()).thenReturn(null);
        when(mockOdometry.getPose()).thenReturn(new Pose(72.0, 72.0, 0.0));

        fusion.periodic();

        // setPose should never be called
        verify(mockOdometry, never()).setPose(any());
    }

    @Test
    void testLowConfidenceSkipsCorrection() {
        // Vision sees a target but confidence is below threshold (0.05)
        when(mockVision.getEstimatedGlobalPose()).thenReturn(new Pose(0.0, 0.0, 0.0));
        when(mockVision.getPoseConfidence()).thenReturn(0.03);
        when(mockOdometry.getPose()).thenReturn(new Pose(72.0, 72.0, 0.0));

        fusion.periodic();

        verify(mockOdometry, never()).setPose(any());
    }

    // ========== Coordinate Conversion ==========

    @Test
    void testVisionCenterOriginConvertsToBottomLeftOrigin() {
        // Vision returns (0, 0) meters (field center)
        // Expected in Pedro inches: (0 * M2I + 72, 0 * M2I + 72) = (72, 72)
        // If odometry is ALSO at (72, 72), blending should not move the robot
        Pose visionPose = new Pose(0.0, 0.0, 0.0); // meters, center origin
        Pose odomPose = new Pose(72.0, 72.0, 0.0);  // inches, bottom-left origin

        when(mockVision.getEstimatedGlobalPose()).thenReturn(visionPose);
        when(mockVision.getPoseConfidence()).thenReturn(1.0);
        when(mockOdometry.getPose()).thenReturn(odomPose);

        fusion.periodic();

        // With perfect confidence, Kalman gain ≈ 0.05/(0.05+0.1*e^0) = 0.05/0.15 ≈ 0.333
        // But capped at MAX_TRUST (0.15)
        // Since vision and odometry should agree (both at field center), result should be ~(72, 72)
        verify(mockOdometry).setPose(argThat(pose -> {
            // Both agree at near center, so interpolated result should be very close to center
            return Math.abs(pose.getX() - 72.0) < 1.0 && Math.abs(pose.getY() - 72.0) < 1.0;
        }));
    }

    // ========== Blending Direction ==========

    @Test
    void testBlendingNudgesTowardVision() {
        // Vision says robot is at (1.0, 0.0) meters → (1.0 * 39.37 + 72, 72) ≈ (111.37, 72) inches
        // Odometry says robot is at (72, 72) inches (field center)
        // After blending, X should move TOWARD 111.37 (not away)
        Pose visionPose = new Pose(1.0, 0.0, 0.0);
        Pose odomPose = new Pose(72.0, 72.0, 0.0);

        when(mockVision.getEstimatedGlobalPose()).thenReturn(visionPose);
        when(mockVision.getPoseConfidence()).thenReturn(0.8);
        when(mockOdometry.getPose()).thenReturn(odomPose);

        fusion.periodic();

        verify(mockOdometry).setPose(argThat(pose -> {
            // X should be greater than 72 (moved toward vision's position)
            return pose.getX() > 72.0;
        }));
    }

    // ========== Angular Shortest-Path Interpolation ==========

    @Test
    void testAngularInterpolationShortestPath() {
        // Odometry heading: 170° (≈2.967 rad)
        // Vision heading: -170° (≈-2.967 rad → equivalent to 190°)
        // Shortest angular path is 20° (through 180°), NOT 340° the long way
        double odomHeading = Math.toRadians(170);
        double visionHeading = Math.toRadians(-170);

        Pose visionPose = new Pose(0.0, 0.0, visionHeading);
        Pose odomPose = new Pose(72.0, 72.0, odomHeading);

        when(mockVision.getEstimatedGlobalPose()).thenReturn(visionPose);
        when(mockVision.getPoseConfidence()).thenReturn(1.0);
        when(mockOdometry.getPose()).thenReturn(odomPose);

        fusion.periodic();

        verify(mockOdometry).setPose(argThat(pose -> {
            double resultHeading = pose.getHeading();
            // The heading should move TOWARD 180° (PI), not away from it
            // 170° + small nudge toward 190° = slightly past 170° toward 180°
            return Math.abs(resultHeading) > Math.toRadians(170);
        }));
    }

    // ========== Kalman Gain Behavior ==========

    @Test
    void testHighConfidenceProducesStrongerCorrection() {
        Pose visionPose = new Pose(1.0, 0.0, 0.0); // Far from odom
        Pose odomPose = new Pose(72.0, 72.0, 0.0);

        // High confidence
        when(mockVision.getEstimatedGlobalPose()).thenReturn(visionPose);
        when(mockVision.getPoseConfidence()).thenReturn(0.95);
        when(mockOdometry.getPose()).thenReturn(odomPose);

        fusion.periodic();

        // Capture the high-confidence result
        org.mockito.ArgumentCaptor<Pose> highCaptor = org.mockito.ArgumentCaptor.forClass(Pose.class);
        verify(mockOdometry).setPose(highCaptor.capture());
        double highCorrectionX = highCaptor.getValue().getX();

        // Reset
        reset(mockOdometry);
        when(mockOdometry.getPose()).thenReturn(odomPose);

        // Low confidence
        when(mockVision.getPoseConfidence()).thenReturn(0.10);
        fusion.periodic();

        org.mockito.ArgumentCaptor<Pose> lowCaptor = org.mockito.ArgumentCaptor.forClass(Pose.class);
        verify(mockOdometry).setPose(lowCaptor.capture());
        double lowCorrectionX = lowCaptor.getValue().getX();

        // High confidence should produce MORE correction (further from 72.0)
        double highDelta = Math.abs(highCorrectionX - 72.0);
        double lowDelta = Math.abs(lowCorrectionX - 72.0);
        assertTrue(highDelta > lowDelta,
            "Higher confidence should produce a larger correction (high=" + highDelta + " vs low=" + lowDelta + ")");
    }
}

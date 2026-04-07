package org.areslib.subsystems.vision;

import org.areslib.command.SubsystemBase;
import org.areslib.core.localization.AresFollower;
import com.pedropathing.geometry.Pose;

/**
 * Sensor Fusion Subsystem orchestrates the mathematical blending of the underlying 
 * Pedro Pathing dead-wheel odometry with absolute global Vision coordinates.
 */
public class AresSensorFusionSubsystem extends SubsystemBase {

    private final AresFollower odometry;
    private final AresVisionSubsystem vision;
    private final double maxVisionTrustFactor;

    /**
     * Constructs the sensor fusion layer.
     * 
     * @param odometry Pedro Pathing wrapped follower
     * @param vision AresVisionSubsystem
     * @param maxVisionTrustFactor The maximum percentage the odometry can be "nudged" toward the vision target per 20ms loop cycle (e.g. 0.15 = 15%).
     */
    public AresSensorFusionSubsystem(AresFollower odometry, AresVisionSubsystem vision, double maxVisionTrustFactor) {
        this.odometry = odometry;
        this.vision = vision;
        this.maxVisionTrustFactor = maxVisionTrustFactor;
    }

    @Override
    public void periodic() {
        Pose visionPose = vision.getEstimatedGlobalPose();
        
        // If the vision system doesn't see anything, we trust the dead-wheels 100%.
        if (visionPose == null) return;

        double confidence = vision.getPoseConfidence();
        
        // Reject extremely noisy or distant measurements
        if (confidence <= 0.05) return;

        Pose currentPose = odometry.getPose();

        // CRITICAL: Vision outputs meters (Center Origin), but Pedro Pathing operates in inches (Bottom-Left Origin).
        // Convert vision coordinates to inches, and shift to Bottom-Left origin before blending.
        // Uses centralized constants to avoid magic-number drift across the codebase.
        double visionXInches = (visionPose.getX() * org.areslib.core.FieldConstants.METERS_TO_INCHES) + org.areslib.core.FieldConstants.HALF_FIELD_INCHES;
        double visionYInches = (visionPose.getY() * org.areslib.core.FieldConstants.METERS_TO_INCHES) + org.areslib.core.FieldConstants.HALF_FIELD_INCHES;
        double visionHeading = visionPose.getHeading(); // Radians are unit-agnostic

        // Calculate Kalman-inspired standard deviations
        // As confidence drops (target area shrinks/distance increases), vision variance explodes exponentially.
        double odomVariance = 0.05; // Base localized tracking variance
        double visionVariance = 0.1 * Math.exp(5.0 * (1.0 - confidence));
        
        // 1D Kalman Gain: K = Var(Odom) / (Var(Odom) + Var(Vision))
        double kalmanGain = odomVariance / (odomVariance + visionVariance);
        
        // Cap the gain dynamically to prevent massive frame-to-frame jumping
        double blendWeight = Math.min(kalmanGain, maxVisionTrustFactor);

        // Push standard deviations to telemetry for Advanced Observability
        org.areslib.telemetry.AresAutoLogger.recordOutput("Vision/KalmanGain", kalmanGain);
        org.areslib.telemetry.AresAutoLogger.recordOutput("Vision/VisionVariance", visionVariance);

        // Perform linear interpolation (lerp) for X and Y coordinates (both in inches now)
        double interpolatedX = currentPose.getX() + (visionXInches - currentPose.getX()) * blendWeight;
        double interpolatedY = currentPose.getY() + (visionYInches - currentPose.getY()) * blendWeight;

        // For headings, use shortest-path angular interpolation to avoid 360° wraparound snapping.
        double currentHeading = currentPose.getHeading();
        
        // Shortest path angle difference
        double angleDifference = visionHeading - currentHeading;
        while (angleDifference > Math.PI) angleDifference -= 2 * Math.PI;
        while (angleDifference < -Math.PI) angleDifference += 2 * Math.PI;

        double interpolatedHeading = currentHeading + (angleDifference * blendWeight);

        // Nudge the core localization follower (all values now in inches + radians)
        odometry.setPose(new Pose(interpolatedX, interpolatedY, interpolatedHeading));
    }
}

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
        double visionXInches = (visionPose.getX() / 0.0254) + 72.0;
        double visionYInches = (visionPose.getY() / 0.0254) + 72.0;
        double visionHeading = visionPose.getHeading(); // Radians are unit-agnostic

        // Calculate dynamic interpolation weight based on camera confidence
        double blendWeight = confidence * maxVisionTrustFactor;

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

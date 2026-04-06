package org.areslib.subsystems.vision;

import org.areslib.command.SubsystemBase;
import org.areslib.telemetry.AresAutoLogger;
import org.areslib.hardware.interfaces.VisionIO;

public class AresVisionSubsystem extends SubsystemBase {
    
    private final VisionIO io;
    private final VisionIO.VisionInputs inputs = new VisionIO.VisionInputs();
    
    private final double minTargetAreaPercent;
    private final double maxTrustAreaPercent;

    /**
     * Constructs a core vision subsystem, parameterized for specific game or camera heuristics.
     *
     * @param io The hardware IO interface (e.g., LimelightVisionWrapper).
     * @param minTargetAreaPercent Absolute minimum target size (% of image) to be considered valid.
     * @param maxTrustAreaPercent Target size (% of image) corresponding to 100% confidence.
     */
    public AresVisionSubsystem(VisionIO io, double minTargetAreaPercent, double maxTrustAreaPercent) {
        this.io = io;
        this.minTargetAreaPercent = minTargetAreaPercent;
        this.maxTrustAreaPercent = maxTrustAreaPercent;
    }

    @Override
    public void periodic() {
        // Automatically fetch network tables or driver inputs
        io.updateInputs(inputs);
        
        // This line performs magic: It automatically diffs the fields within 'inputs' 
        // and pushes the changes across network tables into AdvantageScope for logging.
        AresAutoLogger.processInputs("Vision", inputs);
    }

    public boolean hasTarget() {
        return inputs.hasTarget;
    }

    /**
     * @return Horizontal offset from crosshair to target (Tx) in degrees.
     */
    public double getTargetXOffset() {
        return inputs.tx;
    }

    /**
     * @return Vertical offset from crosshair to target (Ty) in degrees.
     */
    public double getTargetYOffset() {
        return inputs.ty;
    }

    /**
     * @return Target Area (Ta) in percent of image.
     */
    public double getTargetArea() {
        return inputs.ta;
    }

    /**
     * @return Field-centric 2D pose estimated by the vision system. Null if target isn't trustworthy.
     */
    public com.pedropathing.geometry.Pose getEstimatedGlobalPose() {
        if (!inputs.hasTarget) return null;

        // Sanity Check 1: Is the robot floating? 
        // If the vision system thinks the robot's center is floating above the field
        // or buried deep underground, it is a ghost reflection.
        double zElevationMeters = inputs.botPose3d[2];
        if (Math.abs(zElevationMeters) > org.areslib.core.FieldConstants.MAX_ELEVATION_METERS) return null;

        // Sanity Check 2: Are we physically outside the FTC Field?
        // Uses centralized field constants for consistent bounds across the codebase.
        double xMeters = inputs.botPose3d[0];
        double yMeters = inputs.botPose3d[1];
        if (Math.abs(xMeters) > org.areslib.core.FieldConstants.MAX_VISION_POSITION_METERS 
            || Math.abs(yMeters) > org.areslib.core.FieldConstants.MAX_VISION_POSITION_METERS) {
            return null;
        }

        return new com.pedropathing.geometry.Pose(
            xMeters, 
            yMeters, 
            inputs.botPose3d[5] // Yaw
        );
    }
    
    /**
     * Calculates trust coefficient dynamically based on AprilTag latency and visible surface area.
     * @return Raw confidence scale (0.0 to 1.0)
     */
    public double getPoseConfidence() {
        if (!inputs.hasTarget) return 0.0;
        
        // Sanity Check 3: Is it way too small/ambiguous?
        // If the target consumes less than our minimum threshold, we don't trust it enough to blend.
        if (inputs.ta < minTargetAreaPercent) return 0.0;

        // Simple heuristic: larger area = higher confidence. Cap at 1.0.
        // A single tag taking up > maxTrustAreaPercent is very clear and close.
        double confidence = inputs.ta / maxTrustAreaPercent; 
        return Math.min(confidence, 1.0);
    }
    
    public void setPipeline(int index) {
        io.setPipeline(index);
    }
}

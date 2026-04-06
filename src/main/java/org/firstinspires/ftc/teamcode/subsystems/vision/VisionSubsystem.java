package org.firstinspires.ftc.teamcode.subsystems.vision;

import org.areslib.command.Subsystem;
import org.areslib.telemetry.AresAutoLogger;
import org.areslib.hardware.interfaces.VisionIO;

public class VisionSubsystem implements Subsystem {
    
    private final VisionIO io;
    private final VisionIO.VisionInputs inputs = new VisionIO.VisionInputs();

    public VisionSubsystem(VisionIO io) {
        this.io = io;
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
        // If the vision system thinks the robot's center is floating > 0.5 meters in the air
        // or buried deep underground, it is a ghost reflection.
        double zElevationMeters = inputs.botPose3d[2];
        if (zElevationMeters > 0.5 || zElevationMeters < -0.5) return null;

        // Sanity Check 2: Are we physically outside the FTC Field?
        // An FTC field is 12x12 ft (approx 3.65 x 3.65 meters).
        // Assuming Limelight uses center-field (0,0), boundaries are -1.82 to 1.82 meters.
        // We afford a generous 2.5-meter radius buffer before rejecting cleanly.
        double xMeters = inputs.botPose3d[0];
        double yMeters = inputs.botPose3d[1];
        if (Math.abs(xMeters) > 2.5 || Math.abs(yMeters) > 2.5) {
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
        // If the target consumes less than 0.1% of the image, we don't trust it enough to blend.
        if (inputs.ta < 0.1) return 0.0;

        // Simple heuristic: larger area = higher confidence. Cap at 1.0.
        // A single tag taking up > 1.5% of the screen is very clear and close.
        double confidence = inputs.ta / 1.5; 
        return Math.min(confidence, 1.0);
    }
    
    public void setPipeline(int index) {
        io.setPipeline(index);
    }
}

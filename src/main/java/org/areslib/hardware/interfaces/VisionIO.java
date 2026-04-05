package org.areslib.hardware.interfaces;

import org.areslib.telemetry.AresLoggableInputs;

/**
 * AdvantageKit-style IO abstraction for robust vision processing.
 * This can be implemented by a real Limelight/PhotonVision camera or a simulated environment.
 */
public interface VisionIO {
    
    /**
     * Loggable data object containing vision processing state, including
     * target offsets and 3D pose estimates.
     */
    class VisionInputs implements AresLoggableInputs {
        /** True if the vision processor currently sees a valid target. */
        public boolean hasTarget = false;
        
        /** Offset of the primary target in the X axis (degrees). */
        public double tx = 0.0;
        
        /** Offset of the primary target in the Y axis (degrees). */
        public double ty = 0.0;
        
        /** Area of the primary target bounding box (% of image). */
        public double ta = 0.0;
        
        /** 
         * Struct array for the primary Robot Pose calculation (if computing AprilTags).
         * Format matches AdvantageScope 3D Poses (Euler): [x, y, z, roll, pitch, yaw] in meters and radians.
         */
        public double[] botPose3d = new double[6];
        
        /** Optional secondary Pose structure (e.g. Megatag2). [x, y, z, roll, pitch, yaw] */
        public double[] botPoseMegaTag2 = new double[6];

        /** Network or pipeline latency overhead in milliseconds. */
        public double latencyMs = 0.0;
        
        /** The ID of the currently loaded vision pipeline. */
        public int pipelineIndex = 0;
        
        /** Number of active fiducial markers (AprilTags) in frame. */
        public int fiducialCount = 0;
    }

    /**
     * Updates the data structure with the latest values from the underlying hardware sensor or simulation.
     * 
     * @param inputs The VisionInputs object to be populated.
     */
    default void updateInputs(VisionInputs inputs) {}
    
    /**
     * Changes the active vision pipeline execution index.
     *
     * @param index The index of the pipeline to switch to.
     */
    default void setPipeline(int index) {}
}

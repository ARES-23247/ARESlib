package org.areslib.subsystems.vision;

import org.areslib.command.SubsystemBase;
import org.areslib.core.localization.AresFollower;
import org.areslib.hardware.interfaces.ArrayLidarIO;
import org.areslib.hardware.interfaces.ArrayLidarIO.ArrayLidarInputs;
import org.areslib.math.pathing.ObstacleAvoider;
import org.areslib.telemetry.AresAutoLogger;
import com.pedropathing.geometry.Pose;

import java.util.ArrayList;
import java.util.List;

/**
 * Lidar Fusion maps hardware/simulation raycasts back into A-Star field coordinates.
 */
public class AresLidarFusionSubsystem extends SubsystemBase {

    private final ArrayLidarIO io;
    private final ArrayLidarInputs inputs = new ArrayLidarInputs();
    private final AresFollower odometry;
    private final ObstacleAvoider avoider;

    private int tickCounter = 0;
    private static final int CLEAR_INTERVAL_TICKS = 100; // Approx 2 seconds (50Hz)
    private static final double FIELD_BORDER_TOLERANCE_INCHES = 5.0;
    
    // Assumes array covers exactly 45 degrees in front of robot
    private static final double FOV_RADIANS = Math.toRadians(45.0);

    /**
     * Constructs the LiDAR fusion subsystem.
     *
     * @param io       The LiDAR hardware IO interface (real or simulated).
     * @param odometry The PedroPathing follower wrapper for current robot pose.
     * @param avoider  The obstacle avoidance grid to inject detected hit points into.
     */
    public AresLidarFusionSubsystem(ArrayLidarIO io, AresFollower odometry, ObstacleAvoider avoider) {
        this.io = io;
        this.odometry = odometry;
        this.avoider = avoider;
    }

    @Override
    public void periodic() {
        io.updateInputs(inputs);
        AresAutoLogger.processInputs("LiDAR", inputs);

        Pose robotPose = odometry.getPose();
        if (robotPose == null) return;
        
        double rx = robotPose.getX(); // Inches
        double ry = robotPose.getY(); // Inches
        double rHeading = robotPose.getHeading(); // Radians

        tickCounter++;
        if (tickCounter >= CLEAR_INTERVAL_TICKS) {
            avoider.clearDynamicObstacles();
            tickCounter = 0;
        }

        // We only care about objects closer than roughly 2 meters (80 inches) to keep grid stable 
        double maxTrackingDistInches = 80.0;
        int resolution = inputs.distanceZonesMm != null ? inputs.distanceZonesMm.length : 0;
        
        if (resolution == 0) return;

        // AdvantageScope Point Visualization cache
        List<Double> hitPoints = new ArrayList<>();

        int gridDim = (int) Math.sqrt(resolution);
        
        for (int row = 0; row < gridDim; row++) {
            for (int col = 0; col < gridDim; col++) {
                int idx = row * gridDim + col;
                double distInches = inputs.distanceZonesMm[idx] / 25.4;

                // Ignore max range empty hits
                if (distInches >= maxTrackingDistInches) continue;

                // Calculate angle of this specific ray beam
                double colFraction = (col + 0.5) / gridDim - 0.5;
                double yawOffset = colFraction * FOV_RADIANS;
                double mapAngle = rHeading + yawOffset;

                // Project hit point onto Descartes field X,Y
                double hitX = rx + Math.cos(mapAngle) * distInches;
                double hitY = ry + Math.sin(mapAngle) * distInches;

                // If hit was just the physical field wall, DO NOT map it as an obstacle! 
                // Field walls are 0 and 144 inches
                if (hitX < FIELD_BORDER_TOLERANCE_INCHES || hitX > 144.0 - FIELD_BORDER_TOLERANCE_INCHES || 
                    hitY < FIELD_BORDER_TOLERANCE_INCHES || hitY > 144.0 - FIELD_BORDER_TOLERANCE_INCHES) {
                    continue;
                }

                // Add to A* Avoider
                avoider.addDynamicPoint(hitX, hitY);
                
                // Track for telemetry
                hitPoints.add(hitX);
                hitPoints.add(hitY);
            }
        }

        // Serialize double[] array to string for AdvantageScope Points Tab compatibility
        if (!hitPoints.isEmpty()) {
            double[] arr = new double[hitPoints.size()];
            for(int i=0; i<hitPoints.size(); i++) arr[i] = hitPoints.get(i);
            AresAutoLogger.recordOutputArray("LiDAR/FieldHits", arr);
        }
    }
}

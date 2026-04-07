package org.areslib.examples.auto;

import org.areslib.command.Command;
import org.areslib.core.localization.AresFollower;
import org.areslib.hardware.interfaces.ArrayLidarIO;
import org.areslib.subsystems.drive.SwerveDriveSubsystem;

import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

/**
 * Example autonomous command demonstrating dynamic obstacle avoidance using LiDAR and PedroPathing.
 * Detects obstacles in real-time and generates a detour path vector.
 */
public class DynamicAvoidanceAuto extends Command {
    
    private final AresFollower follower;
    private final ArrayLidarIO.ArrayLidarInputs lidarInputs;
    private final Pose startPose = new Pose(0.0, 0.0, 0.0);
    private final Pose targetPose = new Pose(2.0, 0.0, 0.0);
    
    private boolean detourTriggered = false;
    
    /**
     * Constructs a DynamicAvoidanceAuto command.
     * @param drive The drive subsystem.
     * @param follower The robot's path follower.
     * @param lidarInputs The array LiDAR inputs.
     */
    public DynamicAvoidanceAuto(SwerveDriveSubsystem drive, AresFollower follower, ArrayLidarIO.ArrayLidarInputs lidarInputs) {
        this.follower = follower;
        this.lidarInputs = lidarInputs;
        
        // Command requires follower to assert exclusivity
        addRequirements(follower);
    }

    @Override
    public void initialize() {
        this.detourTriggered = false;
        
        // Build initial straight path
        Pose pedroStart = new Pose(startPose.getX(), startPose.getY(), startPose.getHeading());
        Pose pedroTarget = new Pose(targetPose.getX(), targetPose.getY(), targetPose.getHeading());
        
        PathChain initialChain = follower.getFollower().pathBuilder()
                .addPath(new BezierLine(new Pose(pedroStart.getX(), pedroStart.getY(), pedroStart.getHeading()), new Pose(pedroTarget.getX(), pedroTarget.getY(), pedroTarget.getHeading())))
                .setLinearHeadingInterpolation(pedroStart.getHeading(), pedroTarget.getHeading())
                .build();
                
        follower.followPath(initialChain);
    }

    @Override
    public void execute() {
        if (detourTriggered) return; // Already re-routed
        
        if (lidarInputs == null || lidarInputs.distanceZonesMm.length == 0) return;
        
        // Lidar is 64 array (8x8). Row 3, 4 are roughly middle horizon. Cols 3, 4 are center ahead.
        // Let's just scan all 64 zones. If any forward point is < 500mm, we assume obstacle.
        boolean obstacleAhead = false;
        int gridDim = (int) Math.sqrt(lidarInputs.distanceZonesMm.length);
        
        for (int row = 0; row < gridDim; row++) {
            for (int col = 0; col < gridDim; col++) {
                // Focus on the center columns (e.g. out of 8 cols, cols 3 and 4)
                if (col == (gridDim / 2) - 1 || col == (gridDim / 2)) {
                    int index = (row * gridDim) + col;
                    double distMeters = lidarInputs.distanceZonesMm[index] / 1000.0;
                    if (distMeters < 0.5) {
                        obstacleAhead = true;
                        break;
                    }
                }
            }
            if (obstacleAhead) break;
        }
        
        if (obstacleAhead) {
            System.out.println("OBSTACLE DETECTED! Rerouting trajectory...");
            detourTriggered = true;
            follower.breakFollowing();
            
            Pose currentPose = follower.getPose();
            
            // Build a detour curve
            // Tangent: push out radially to the left (Y + 0.8m) and forward (X + 0.5m)
            Pose currentPt = new Pose(currentPose.getX(), currentPose.getY());
            Pose detourPt = new Pose(currentPose.getX() + 0.5, currentPose.getY() + 0.8);
            Pose pedroTarget = new Pose(targetPose.getX(), targetPose.getY(), targetPose.getHeading());
            Pose targetPt = new Pose(pedroTarget.getX(), pedroTarget.getY(), pedroTarget.getHeading());
            
            PathChain detourChain = follower.getFollower().pathBuilder()
                .addPath(new BezierCurve(currentPt, detourPt, targetPt))
                .setLinearHeadingInterpolation(currentPose.getHeading(), pedroTarget.getHeading())
                .build();
                
            follower.followPath(detourChain);
        }
    }

    @Override
    public boolean isFinished() {
        return !follower.isBusy();
    }
}

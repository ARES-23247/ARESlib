package org.areslib.command.auto;

import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import org.areslib.command.Command;
import org.areslib.core.localization.AresFollower;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;

import java.util.function.Supplier;

/**
 * An advanced autonomous command that generates PedroPathing trajectories On-The-Fly (OTF).
 * Automatically dodges a known dynamic obstacle by calculating an optimal Bezier curving 
 * control point to push the trajectory away from the collision radius.
 */
public class DynamicPathCommand extends Command {

    private final AresFollower m_follower;
    private final Supplier<Translation2d> m_obstaclePositionSupplier;
    private final Translation2d m_targetPosition;
    private final Rotation2d m_targetHeading;
    
    private final double m_safeRadiusMeters;
    private final double m_dodgeMarginMeters;

    private boolean m_pathGenerated = false;

    /**
     * Constructs a dynamic pathing command with obstacle avoidance.
     *
     * @param follower                 The AresFollower subsystem.
     * @param targetPosition           The final destination translation (in meters).
     * @param targetHeading            The final destination heading.
     * @param obstaclePositionSupplier A supplier providing the real-time position of the dynamic obstacle.
     *                                 (e.g., from a Vision subsystem or another robot's known telemetry).
     * @param safeRadiusMeters         The collision radius of the obstacle.
     * @param dodgeMarginMeters        Additional padding distance to give the obstacle.
     */
    public DynamicPathCommand(AresFollower follower, 
                              Translation2d targetPosition, 
                              Rotation2d targetHeading,
                              Supplier<Translation2d> obstaclePositionSupplier, 
                              double safeRadiusMeters, 
                              double dodgeMarginMeters) {
        m_follower = follower;
        m_targetPosition = targetPosition;
        m_targetHeading = targetHeading;
        m_obstaclePositionSupplier = obstaclePositionSupplier;
        m_safeRadiusMeters = safeRadiusMeters;
        m_dodgeMarginMeters = dodgeMarginMeters;
        
        addRequirements(follower);
    }

    @Override
    public void initialize() {
        m_pathGenerated = false;
        
        // Convert to Pedro Coordinates (Inches for internal Pedro logic typically, 
        // but AresFollower wrappers handle the units mapping, assuming meters mapped to standard pose)
        // Note: Pedro native usually runs in Inches. AresFollower injects SI meters into standard wrappers.
        Pose startPose = m_follower.getPose();
        Translation2d start = new Translation2d(startPose.getX(), startPose.getY());
        Translation2d obstacle = m_obstaclePositionSupplier.get();
        
        PathChain dynamicChain;
        
        if (obstacle == null) {
            // No obstacle, simple straight line
            dynamicChain = buildStraight(startPose);
        } else {
            // Calculate closest point on line segment start -> target
            Translation2d v = m_targetPosition.minus(start);
            Translation2d w = obstacle.minus(start);
            
            double dSq = v.getX() * v.getX() + v.getY() * v.getY();
            if (dSq < 0.001) {
                // Already at target essentially
                dynamicChain = buildStraight(startPose);
            } else {
                double t = (w.getX() * v.getX() + w.getY() * v.getY()) / dSq;
                t = Math.max(0, Math.min(1, t)); // Clamp to segment
                
                Translation2d closestPoint = start.plus(new Translation2d(t * v.getX(), t * v.getY()));
                Translation2d distVec = closestPoint.minus(obstacle); // Vector from obstacle TO closest path point
                
                double dist = Math.hypot(distVec.getX(), distVec.getY());
                
                if (dist < m_safeRadiusMeters) {
                    // Collision imminent. Calculate a Bezier control point to push the curve out.
                    Translation2d normal;
                    if (dist < 0.001) {
                        // Obstacle is exactly on the line. Push arbitrarily perpendicular to the path.
                        normal = new Translation2d(-v.getY(), v.getX()); 
                    } else {
                        normal = distVec;
                    }
                    
                    // Normalize the outward push vector
                    double normLen = Math.hypot(normal.getX(), normal.getY());
                    Translation2d unitNormal = new Translation2d(normal.getX() / normLen, normal.getY() / normLen);
                    
                    // The detour control point
                    double pushDistance = m_safeRadiusMeters + m_dodgeMarginMeters;
                    Translation2d controlPt = obstacle.plus(new Translation2d(unitNormal.getX() * pushDistance, unitNormal.getY() * pushDistance));
                    
                    dynamicChain = buildCurve(startPose, controlPt);
                } else {
                    // Path is clear
                    dynamicChain = buildStraight(startPose);
                }
            }
        }

        m_follower.followPath(dynamicChain);
        m_pathGenerated = true;
    }

    private PathChain buildStraight(Pose startPose) {
        Pose pedroTarget = new Pose(m_targetPosition.getX(), m_targetPosition.getY(), m_targetHeading.getRadians());
        return m_follower.getFollower().pathBuilder()
                .addPath(new com.pedropathing.geometry.BezierLine(
                    new com.pedropathing.geometry.Pose(startPose.getX(), startPose.getY()), 
                    new com.pedropathing.geometry.Pose(pedroTarget.getX(), pedroTarget.getY())
                ))
                .setLinearHeadingInterpolation(startPose.getHeading(), pedroTarget.getHeading())
                .build();
    }

    private PathChain buildCurve(Pose startPose, Translation2d controlPoint) {
        Pose pedroTarget = new Pose(m_targetPosition.getX(), m_targetPosition.getY(), m_targetHeading.getRadians());
        Pose pedroControl = new Pose(controlPoint.getX(), controlPoint.getY());
        
        return m_follower.getFollower().pathBuilder()
                .addPath(new BezierCurve(
                    new Pose(startPose.getX(), startPose.getY()), 
                    pedroControl, 
                    new Pose(pedroTarget.getX(), pedroTarget.getY())
                ))
                .setTangentHeadingInterpolation()
                .build();
    }

    @Override
    public void execute() {
        // Active following is handled strictly continuously by AresFollower periodic updates.
    }

    @Override
    public boolean isFinished() {
        return m_pathGenerated && !m_follower.isBusy();
    }
}

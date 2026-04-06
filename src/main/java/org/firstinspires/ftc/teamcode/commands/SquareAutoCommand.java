package org.firstinspires.ftc.teamcode.commands;

import org.areslib.command.Command;
import org.areslib.core.localization.AresFollower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

public class SquareAutoCommand extends Command {
    private final AresFollower aresFollower;
    private final PathChain[] squareSegments = new PathChain[4];
    private int segmentIndex = 0;

    public SquareAutoCommand(AresFollower follower) {
        this.aresFollower = follower;
        addRequirements(follower);

        // Define 4 individual segments for the square. We will command the robot to stop at the end of each.
        squareSegments[0] = follower.getFollower().pathBuilder()
            .addPath(new BezierLine(new Pose(72, 72, 0), new Pose(96, 72, 0)))
            .setLinearHeadingInterpolation(0, Math.PI / 2)
            .build();
            
        squareSegments[1] = follower.getFollower().pathBuilder()
            .addPath(new BezierLine(new Pose(96, 72, 0), new Pose(96, 96, 0)))
            .setLinearHeadingInterpolation(Math.PI / 2, Math.PI)
            .build();
            
        squareSegments[2] = follower.getFollower().pathBuilder()
            .addPath(new BezierLine(new Pose(96, 96, 0), new Pose(72, 96, 0)))
            .setLinearHeadingInterpolation(Math.PI, 3 * Math.PI / 2)
            .build();
            
        squareSegments[3] = follower.getFollower().pathBuilder()
            .addPath(new BezierLine(new Pose(72, 96, 0), new Pose(72, 72, 0)))
            .setLinearHeadingInterpolation(3 * Math.PI / 2, 2 * Math.PI)
            .build();
    }

    private boolean isRoutingToStart = false;

    @Override
    public void initialize() {
        Pose currentPose = aresFollower.getPose();
        Pose startPose = new Pose(72, 72, 0);

        segmentIndex = 0;

        // If the robot is more than 2 inches away from the start pose, route it there first intelligently.
        // Don't reset the pose, just use real-time coordinate data!
        if (currentPose.distanceFrom(startPose) > 2.0) {
            isRoutingToStart = true;
            PathChain routingChain = aresFollower.getFollower().pathBuilder()
                .addPath(new BezierLine(currentPose, startPose))
                .setLinearHeadingInterpolation(currentPose.getHeading(), startPose.getHeading())
                .build();
            aresFollower.followPath(routingChain, true);
        } else {
            isRoutingToStart = false;
            // Begin following the first segment
            aresFollower.followPath(squareSegments[segmentIndex], true);
        }
    }

    @Override
    public void execute() {
        // If the path follower completes the segment and reaches the target pose, proceed to the next.
        if (!aresFollower.isBusy()) {
            if (isRoutingToStart) {
                isRoutingToStart = false;
                aresFollower.followPath(squareSegments[segmentIndex], true);
            } else {
                segmentIndex = (segmentIndex + 1) % 4;
                aresFollower.followPath(squareSegments[segmentIndex], true);
            }
        }
    }

    @Override
    public boolean isFinished() {
        // Autonomous routine never exits freely unless interrupted by the GUI toggle
        return false;
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            aresFollower.breakFollowing();
        }
    }
}

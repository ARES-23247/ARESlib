package org.areslib.command;

import org.areslib.core.localization.AresFollower;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;

/**
 * A command that binds the AresFollower path execution to the Command-Based framework.
 * This class handles starting, monitoring, and cleaning up a PedroPathing trajectory.
 */
public class FollowPathCommand extends Command {

    private final AresFollower follower;
    private Path path = null;
    private PathChain pathChain = null;
    private final boolean holdEnd;

    /**
     * Creates a new FollowPathCommand for a single path.
     *
     * @param follower The AresFollower instance to execute the path.
     * @param path The single Path to follow.
     */
    public FollowPathCommand(AresFollower follower, Path path) {
        this(follower, path, false);
    }

    /**
     * Creates a new FollowPathCommand for a single path with hold option.
     *
     * @param follower The AresFollower instance to execute the path.
     * @param path The single Path to follow.
     * @param holdEnd Whether the robot should actively hold its final position using PID.
     */
    public FollowPathCommand(AresFollower follower, Path path, boolean holdEnd) {
        this.follower = follower;
        this.path = path;
        this.holdEnd = holdEnd;
        addRequirements(follower);
    }

    /**
     * Creates a new FollowPathCommand for a sequenced chain of paths.
     *
     * @param follower The AresFollower instance to execute the path chain.
     * @param pathChain The PathChain to follow continuously.
     */
    public FollowPathCommand(AresFollower follower, PathChain pathChain) {
        this(follower, pathChain, false);
    }

    /**
     * Creates a new FollowPathCommand for a sequenced chain of paths with hold option.
     *
     * @param follower The AresFollower instance to execute the path chain.
     * @param pathChain The PathChain to follow continuously.
     * @param holdEnd Whether the robot should actively hold its final position using PID.
     */
    public FollowPathCommand(AresFollower follower, PathChain pathChain, boolean holdEnd) {
        this.follower = follower;
        this.pathChain = pathChain;
        this.holdEnd = holdEnd;
        addRequirements(follower);
    }

    @Override
    public void initialize() {
        if (path != null) {
            follower.followPath(path, holdEnd);
        } else if (pathChain != null) {
            follower.followPath(pathChain, holdEnd);
        }
    }

    @Override
    public void execute() {
        // The follower logic is updated in AresFollower.periodic()
        // No explicit update needed here, but we could hook telemetry if desired
    }

    @Override
    public boolean isFinished() {
        return !follower.isBusy();
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted || !holdEnd) {
            follower.breakFollowing();
        }
    }
}

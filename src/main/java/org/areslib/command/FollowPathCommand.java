package org.areslib.command;

import org.areslib.command.Command;
import org.areslib.core.localization.AresFollower;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;

public class FollowPathCommand extends Command {

    private final AresFollower follower;
    private Path path = null;
    private PathChain pathChain = null;
    private final boolean holdEnd;

    public FollowPathCommand(AresFollower follower, Path path) {
        this(follower, path, false);
    }

    public FollowPathCommand(AresFollower follower, Path path, boolean holdEnd) {
        this.follower = follower;
        this.path = path;
        this.holdEnd = holdEnd;
        addRequirements(follower);
    }

    public FollowPathCommand(AresFollower follower, PathChain pathChain) {
        this(follower, pathChain, false);
    }

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

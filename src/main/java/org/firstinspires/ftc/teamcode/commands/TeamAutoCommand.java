package org.firstinspires.ftc.teamcode.commands;

import org.areslib.command.Command;
import org.areslib.command.CommandScheduler;
import org.areslib.core.localization.AresFollower;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorSubsystem;
import static org.firstinspires.ftc.teamcode.Constants.ElevatorConstants.HIGH_POSITION_METERS;

import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

public class TeamAutoCommand extends Command {
    private final AresFollower follower;
    private final ElevatorSubsystem elevator;
    private int state = 0;
    
    private final PathChain toScore;
    private final PathChain toPark;
    
    private ElevatorToPositionCommand elevatorCommand;
    private boolean elevatorScheduled = false;

    public TeamAutoCommand(AresFollower follower, ElevatorSubsystem elevator) {
        this.follower = follower;
        this.elevator = elevator;
        addRequirements(follower); // Only claim follower — elevator is scheduled separately
        
        // Define poses (X, Y, Heading radians)
        Pose startPose = new Pose(0, 0, Math.toRadians(0));
        Pose scorePose = new Pose(24, 0, Math.toRadians(0)); 
        Pose parkPose = new Pose(24, 24, Math.toRadians(90)); 
        
        // Build path chains using BezierLine for straight-line segments
        toScore = follower.getFollower().pathBuilder()
            .addPath(new BezierLine(startPose, scorePose))
            .setLinearHeadingInterpolation(startPose.getHeading(), scorePose.getHeading())
            .build();
            
        toPark = follower.getFollower().pathBuilder()
            .addPath(new BezierLine(scorePose, parkPose))
            .setLinearHeadingInterpolation(scorePose.getHeading(), parkPose.getHeading())
            .build();
    }

    @Override
    public void initialize() {
        state = 0;
        elevatorScheduled = false;
        // Set the starting pose when the command actually runs, not when constructed
        follower.getFollower().setStartingPose(new Pose(0, 0, Math.toRadians(0)));
    }

    @Override
    public void execute() {
        switch(state) {
            case 0:
                follower.followPath(toScore, true); // Hold end position
                state = 1;
                break;
            case 1:
                if (!follower.isBusy()) {
                    // Reached scoring position — schedule the elevator via the CommandScheduler
                    elevatorCommand = new ElevatorToPositionCommand(elevator, HIGH_POSITION_METERS);
                    CommandScheduler.getInstance().schedule(elevatorCommand);
                    elevatorScheduled = true;
                    state = 2;
                }
                break;
            case 2:
                // Wait for the elevator command to be removed from the scheduler (finished naturally)
                // Using isScheduled() instead of isFinished() to avoid a one-cycle race
                // where the scheduler removes the command in the same tick we check.
                if (elevatorScheduled && !CommandScheduler.getInstance().isScheduled(elevatorCommand)) {
                    elevatorScheduled = false;
                    // Elevator is up, move to park
                    follower.followPath(toPark, false);
                    state = 3;
                }
                break;
            case 3:
                if (!follower.isBusy()) {
                    // Reached park position
                    state = 4;
                }
                break;
        }
    }

    @Override
    public boolean isFinished() {
        return state == 4;
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            follower.breakFollowing();
            // Cancel the elevator if it's still running
            if (elevatorScheduled && elevatorCommand != null) {
                CommandScheduler.getInstance().cancel(elevatorCommand);
            }
        }
    }
}

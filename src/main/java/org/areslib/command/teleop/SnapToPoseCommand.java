package org.areslib.command.teleop;

import org.areslib.command.Command;
import org.areslib.command.Subsystem;
import org.areslib.core.localization.AresFollower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.paths.PathChain;

import java.util.function.Supplier;

/**
 * A highly advanced teleop assist command. 
 * <p>
 * When bound to a button (e.g., {@code gamepad.rightBumper().whileTrue(...)}), this command interrupts the 
 * standard driver controlled Teleop command. It pulls the robot's current pose, dynamically calculates a smooth 
 * Pedro Pathing vector line to a provided target score pose (such as an AprilTag alignment configuration), 
 * and automatically drives the robot perfectly to that pose. 
 * <p>
 * When the button is released, the command ends, removing the Drivetrain interrupt and seamlessly returning
 * control back to the human driver.
 */
public class SnapToPoseCommand extends Command {

    private final AresFollower m_follower;
    private final Supplier<Pose> m_targetPoseSupplier;

    public SnapToPoseCommand(AresFollower follower, Pose exactTargetPose, Subsystem drivetrainRequirement) {
        this(follower, () -> exactTargetPose, drivetrainRequirement);
    }

    public SnapToPoseCommand(AresFollower follower, Supplier<Pose> targetPoseSupplier, Subsystem drivetrainRequirement) {
        m_follower = follower;
        m_targetPoseSupplier = targetPoseSupplier;

        // Requires the drivetrain so that the default Teleop Drive command is gracefully interrupted!
        addRequirements(drivetrainRequirement);
    }

    @Override
    public void initialize() {
        Pose startPose = m_follower.getPose();
        Pose targetPose = m_targetPoseSupplier.get();

        // Dynamically build an on-the-fly path to the target!
        PathChain dynamicPath = m_follower.getFollower().pathBuilder()
            .addPath(new BezierLine(startPose, targetPose))
            .setLinearHeadingInterpolation(startPose.getHeading(), targetPose.getHeading())
            .build();

        m_follower.followPath(dynamicPath, true); // True to hold end position when we reach it
    }

    @Override
    public void execute() {
        // The follower needs continuous updating. Since the follower typically updates natively in the 
        // internal localization loop, we don't necessarily have to call update() here if AresRobot loops it.
    }

    @Override
    public boolean isFinished() {
        return !m_follower.isBusy();
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            m_follower.breakFollowing();
        }
    }
}

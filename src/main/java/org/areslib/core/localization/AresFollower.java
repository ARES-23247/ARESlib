package org.areslib.core.localization;

import org.areslib.command.SubsystemBase;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.subsystems.drive.DriveSubsystem;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;

public class AresFollower extends SubsystemBase {
    
    private final Follower follower;

    public AresFollower(DriveSubsystem driveSubsystem, OdometryIO.OdometryInputs odometryInputs) {
        // Build dependencies for the Pedro Follower
        AresPedroLocalizer localizer = new AresPedroLocalizer(odometryInputs);
        AresPedroDrivetrain drivetrain = new AresPedroDrivetrain(driveSubsystem);
        FollowerConstants constants = AresPedroConstants.createConstants();
        
        // Instantiate Pedro Follower
        this.follower = new Follower(constants, localizer, drivetrain);
    }

    public void followPath(Path path) {
        this.follower.followPath(path);
    }

    public void followPath(Path path, boolean holdEnd) {
        this.follower.followPath(path, holdEnd);
    }

    public void followPath(PathChain pathChain) {
        this.follower.followPath(pathChain);
    }

    public void followPath(PathChain pathChain, boolean holdEnd) {
        this.follower.followPath(pathChain, holdEnd);
    }

    public boolean isBusy() {
        return this.follower.isBusy();
    }

    public void breakFollowing() {
        this.follower.breakFollowing();
    }

    @Override
    public void periodic() {
        // Crucial: Update the follower logic every schedule loop
        this.follower.update();
    }
}

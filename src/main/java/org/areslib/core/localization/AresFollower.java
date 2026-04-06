package org.areslib.core.localization;

import org.areslib.command.SubsystemBase;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.subsystems.drive.SwerveDriveSubsystem;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;

/**
 * ARESlib integration layer for Pedro Pathing's {@link Follower}.
 * 
 * <p>This subsystem handles the initialization and periodic updating of the Pedro
 * Pathing Follower, mapping ARESLib's localizer and drivetrain implementations
 * into the Pedro trajectory tracking environment.
 */
public class AresFollower extends SubsystemBase {
    
    private final Follower follower;

    /**
     * Constructs a new AresFollower subsystem.
     *
     * @param driveSubsystem The ARES drivetrain subsystem used to actuate the robot.
     * @param odometryInputs The odometry hardware inputs used for pose estimation.
     */
    public AresFollower(SwerveDriveSubsystem driveSubsystem, OdometryIO.OdometryInputs odometryInputs) {
        // Build dependencies for the Pedro Follower
        AresPedroLocalizer localizer = new AresPedroLocalizer(odometryInputs);
        AresPedroDrivetrain drivetrain = new AresPedroDrivetrain(driveSubsystem);
        FollowerConstants constants = AresPedroConstants.createConstants();
        
        // Instantiate Pedro Follower
        this.follower = new Follower(constants, localizer, drivetrain);
    }

    /**
     * Gets the current estimated pose array mapped from the localizer.
     *
     * @return The current robot {@link com.pedropathing.geometry.Pose}.
     */
    public com.pedropathing.geometry.Pose getPose() {
        return this.follower.getPose();
    }
    
    /**
     * Overrides the current tracked pose of the underlying localizer.
     *
     * @param pose The new {@link com.pedropathing.geometry.Pose} to lock tracking to.
     */
    public void setPose(com.pedropathing.geometry.Pose pose) {
        this.follower.setPose(pose);
    }

    /**
     * Retrieves the underlying Pedro Pathing Follower instance.
     *
     * @return The raw {@link Follower} instance.
     */
    public Follower getFollower() {
        return this.follower;
    }

    /**
     * Commands the follower to follow the given single path.
     *
     * @param path The {@link Path} for the robot to follow.
     */
    public void followPath(Path path) {
        this.follower.followPath(path);
    }

    /**
     * Commands the follower to follow the given single path, optionally holding position at the end.
     *
     * @param path    The {@link Path} for the robot to follow.
     * @param holdEnd Whether the robot should actively hold its final pose once the path completes.
     */
    public void followPath(Path path, boolean holdEnd) {
        this.follower.followPath(path, holdEnd);
    }

    /**
     * Commands the follower to follow the given path chain (series of paths).
     *
     * @param pathChain The {@link PathChain} for the robot to follow.
     */
    public void followPath(PathChain pathChain) {
        this.follower.followPath(pathChain);
    }

    /**
     * Commands the follower to follow the given path chain, optionally holding position at the end.
     *
     * @param pathChain The {@link PathChain} for the robot to follow.
     * @param holdEnd   Whether the robot should actively hold its final pose once the chain completes.
     */
    public void followPath(PathChain pathChain, boolean holdEnd) {
        this.follower.followPath(pathChain, holdEnd);
    }

    /**
     * Checks if the follower is currently executing a path.
     *
     * @return True if a path is actively being followed, false otherwise.
     */
    public boolean isBusy() {
        return this.follower.isBusy();
    }

    /**
     * Interrupts and cancels any active path following routine, causing the robot to stop.
     */
    public void breakFollowing() {
        this.follower.breakFollowing();
    }

    /**
     * Updates the underlying follower calculation loop.
     * This is called automatically every cycle by the {@link org.areslib.command.CommandScheduler}.
     */
    @Override
    public void periodic() {
        // Crucial: Update the follower logic every schedule loop
        this.follower.update();

        // Pinpoint Glitch Protection (Hardware I2C Fault Tolerance)
        // Uses centralized field constants for consistent bounds across the codebase.
        com.pedropathing.geometry.Pose pose = this.follower.getPose();
        boolean outOfBounds = false;
        double clampedX = pose.getX();
        double clampedY = pose.getY();

        if (clampedX < org.areslib.core.FieldConstants.MIN_POSITION_INCHES) { 
            clampedX = org.areslib.core.FieldConstants.MIN_POSITION_INCHES; outOfBounds = true; 
        } else if (clampedX > org.areslib.core.FieldConstants.MAX_POSITION_INCHES) { 
            clampedX = org.areslib.core.FieldConstants.MAX_POSITION_INCHES; outOfBounds = true; 
        }

        if (clampedY < org.areslib.core.FieldConstants.MIN_POSITION_INCHES) { 
            clampedY = org.areslib.core.FieldConstants.MIN_POSITION_INCHES; outOfBounds = true; 
        } else if (clampedY > org.areslib.core.FieldConstants.MAX_POSITION_INCHES) { 
            clampedY = org.areslib.core.FieldConstants.MAX_POSITION_INCHES; outOfBounds = true; 
        }

        // If I2C hardware noise spiked the position out of bounds, forcefully clamp it 
        // to prevent autonomous trajectories from generating impossible vectors.
        if (outOfBounds) {
            this.follower.setPose(new com.pedropathing.geometry.Pose(clampedX, clampedY, pose.getHeading()));
        }
    }
}

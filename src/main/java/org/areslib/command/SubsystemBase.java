package org.areslib.command;

/**
 * A base for subsystems that provides a more intuitive class hierarchy.
 * <p>
 * <b>Important:</b> Subsystems must be explicitly registered with the
 * {@link CommandScheduler} via {@code CommandScheduler.getInstance().registerSubsystem(this)}.
 * This is typically done in the {@code RobotContainer} after construction.
 */
public abstract class SubsystemBase implements Subsystem {

    /**
     * Default constructor. Does NOT auto-register with the scheduler.
     * Call {@code CommandScheduler.getInstance().registerSubsystem(this)} in your RobotContainer.
     */
    public SubsystemBase() {
        // Intentionally empty — explicit registration is required
    }

}

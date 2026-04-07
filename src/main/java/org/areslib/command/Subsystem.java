package org.areslib.command;

/**
 * A robot subsystem. Subsystems are the basic building blocks of the command-based framework;
 * they encapsulate hardware devices and provide methods for commands to interact with them.
 * <p>
 * Subsystems must be registered with the {@link CommandScheduler} for their {@link #periodic()}
 * method to be called. In simulation mode, {@link #simulationPeriodic()} is also called each tick.
 */
public interface Subsystem {
    /**
     * This method is called periodically by the CommandScheduler. Useful for updating
     * subsystem-specific state that you don't want to defer to a Command.
     */
    default void periodic() {}

    /**
     * This method is called periodically by the CommandScheduler. Useful for updating
     * simulation-specific state that you don't want to defer to a Command.
     */
    default void simulationPeriodic() {}
}

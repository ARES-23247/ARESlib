package org.areslib.command.button;

import org.areslib.command.Command;
import org.areslib.command.CommandScheduler;

import java.util.function.BooleanSupplier;

/**
 * A class used to bind command execution to a boolean condition (such as a gamepad button press).
 * 
 * <p>Triggers continuously evaluate their boolean condition and can schedule or cancel
 * commands in the {@link CommandScheduler} when their state changes.
 */
public class Trigger {

    private final BooleanSupplier condition;
    private boolean previousState = false;

    /**
     * Constructs a new Trigger based on a boolean condition.
     *
     * @param condition The condition that determines the active state of the trigger.
     */
    public Trigger(BooleanSupplier condition) {
        this.condition = condition;
    }

    /**
     * Retrieves the current state of the trigger condition.
     *
     * @return True if the condition is met, false otherwise.
     */
    public boolean getAsBoolean() {
        return condition.getAsBoolean();
    }

    /**
     * Starts the given command whenever the trigger just becomes active.
     * <p>
     * The command is scheduled on the rising edge of the condition (i.e. changing from false to true).
     *
     * @param command The command to start.
     * @return This trigger, for builder-style method chaining.
     */
    public Trigger onTrue(Command command) {
        CommandScheduler.getInstance().addButton(() -> {
            boolean currentState = getAsBoolean();
            if (currentState && !previousState) {
                CommandScheduler.getInstance().schedule(command);
            }
            previousState = currentState;
        });
        return this;
    }

    /**
     * Starts the given command when the trigger initially becomes active, and cancels it when it becomes inactive.
     * <p>
     * The command is scheduled on the rising edge and cancelled on the falling edge of the condition.
     *
     * @param command The command to start and cancel.
     * @return This trigger, for builder-style method chaining.
     */
    public Trigger whileTrue(Command command) {
        CommandScheduler.getInstance().addButton(() -> {
            boolean currentState = getAsBoolean();
            if (currentState && !previousState) {
                CommandScheduler.getInstance().schedule(command);
            } else if (!currentState && previousState) {
                CommandScheduler.getInstance().cancel(command);
            }
            previousState = currentState;
        });
        return this;
    }
}

package org.areslib.command.button;

import org.areslib.command.Command;
import org.areslib.command.CommandScheduler;

import java.util.function.BooleanSupplier;

public class Trigger {

    private final BooleanSupplier condition;
    private boolean previousState = false;

    public Trigger(BooleanSupplier condition) {
        this.condition = condition;
    }

    public boolean getAsBoolean() {
        return condition.getAsBoolean();
    }

    /**
     * Starts the given command whenever the trigger just becomes active.
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

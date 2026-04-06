package org.areslib.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A CommandGroup that runs a set of commands in parallel, ending when the last command ends.
 *
 * <p>As a rule, CommandGroups require the union of the requirements of their component commands.
 */
public class ParallelCommandGroup extends Command {
    // Map of commands to whether they have finished
    private final Map<Command, Boolean> m_commands = new HashMap<>();

    /**
     * Creates a new ParallelCommandGroup. The given commands will be executed simultaneously.
     * The command group will finish when the last command finishes.
     *
     * @param commands the commands to include in this group.
     */
    public ParallelCommandGroup(Command... commands) {
        addCommands(commands);
    }

    public final void addCommands(Command... commands) {
        if (!m_commands.isEmpty() && isRunning()) {
            throw new IllegalStateException("Commands cannot be added to a CommandGroup while the group is running");
        }

        for (Command command : commands) {
            if (!Collections.disjoint(command.getRequirements(), m_requirements)) {
                throw new IllegalArgumentException("Multiple commands in a parallel group cannot require the same subsystems");
            }
            m_commands.put(command, false);
            m_requirements.addAll(command.getRequirements());
        }
    }

    private boolean isRunning() {
        // Simple check just to prevent adding commands while active
        for (Boolean finished : m_commands.values()) {
            if (!finished) return true; // If any are not finished, assume active if we passed init
        }
        return false;
    }

    @Override
    public void initialize() {
        for (Map.Entry<Command, Boolean> commandRun : m_commands.entrySet()) {
            commandRun.getKey().initialize();
            commandRun.setValue(false);
        }
    }

    @Override
    public void execute() {
        for (Map.Entry<Command, Boolean> commandRun : m_commands.entrySet()) {
            if (!commandRun.getValue()) {
                commandRun.getKey().execute();
                if (commandRun.getKey().isFinished()) {
                    commandRun.getKey().end(false);
                    commandRun.setValue(true);
                }
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            for (Map.Entry<Command, Boolean> commandRun : m_commands.entrySet()) {
                if (!commandRun.getValue()) {
                    commandRun.getKey().end(true);
                }
            }
        }
    }

    @Override
    public boolean isFinished() {
        for (Boolean finished : m_commands.values()) {
            if (!finished) {
                return false;
            }
        }
        return true;
    }
}

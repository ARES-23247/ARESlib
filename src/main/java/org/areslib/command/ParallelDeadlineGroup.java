package org.areslib.command;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A CommandGroup that runs a set of commands in parallel, ending only when a specific command
 * (the "deadline") ends, interrupting all other commands that are still running at that point.
 */
public class ParallelDeadlineGroup extends Command {
    private final Set<Command> m_commands = new HashSet<>();
    private Command m_deadline;
    private boolean m_finished = false;

    /**
     * Creates a new ParallelDeadlineGroup. The given commands will be executed simultaneously.
     * The group will finish when the deadline command finishes.
     *
     * @param deadline The command that determines when the group ends.
     * @param commands The other commands to be executed.
     */
    public ParallelDeadlineGroup(Command deadline, Command... commands) {
        m_deadline = deadline;
        addCommands(deadline);
        addCommands(commands);
    }

    /**
     * Adds the given commands to the group.
     *
     * @param commands Commands to add.
     */
    public final void addCommands(Command... commands) {
        if (m_finished) {
            throw new IllegalStateException("Commands cannot be added to a CommandGroup while the group is running");
        }

        for (Command command : commands) {
            if (!Collections.disjoint(command.getRequirements(), m_requirements)) {
                throw new IllegalArgumentException("Multiple commands in a parallel group cannot require the same subsystems");
            }
            m_commands.add(command);
            m_requirements.addAll(command.getRequirements());
        }
    }

    /**
     * Sets the deadline to the given command.
     *
     * @param deadline the command that determines when the group ends.
     */
    public void setDeadline(Command deadline) {
        if (!m_commands.contains(deadline)) {
            addCommands(deadline);
        }
        m_deadline = deadline;
    }

    @Override
    public void initialize() {
        m_finished = false;
        for (Command command : m_commands) {
            command.initialize();
        }
    }

    @Override
    public void execute() {
        for (Command command : m_commands) {
            command.execute();
            if (command == m_deadline && command.isFinished()) {
                m_finished = true;
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        for (Command command : m_commands) {
            if (command == m_deadline) {
                // Deadline finished naturally (or the whole group was interrupted)
                command.end(interrupted);
            } else {
                // Other commands are always interrupted when the deadline finishes
                command.end(true);
            }
        }
    }

    @Override
    public boolean isFinished() {
        return m_finished;
    }
}

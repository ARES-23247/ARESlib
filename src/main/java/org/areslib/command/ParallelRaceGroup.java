package org.areslib.command;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A CommandGroup that runs a set of commands in parallel, ending when any one of the commands ends
 * and interrupting all the others.
 */
public class ParallelRaceGroup extends Command {
  private final Set<Command> m_commands = new HashSet<>();
  private boolean m_finished = false;
  private Command m_winner = null;

  /**
   * Creates a new ParallelRaceGroup. The given commands will be executed simultaneously, and will
   * "race" to the finish.
   *
   * @param commands the commands to include in this group.
   */
  public ParallelRaceGroup(Command... commands) {
    addCommands(commands);
  }

  /**
   * Adds the given commands to the group.
   *
   * @param commands Commands to add.
   */
  public final void addCommands(Command... commands) {
    if (m_finished) {
      throw new IllegalStateException(
          "Commands cannot be added to a CommandGroup while the group is running");
    }

    for (Command command : commands) {
      if (!Collections.disjoint(command.getRequirements(), m_requirements)) {
        throw new IllegalArgumentException(
            "Multiple commands in a parallel group cannot require the same subsystems");
      }
      m_commands.add(command);
      m_requirements.addAll(command.getRequirements());
    }
  }

  @Override
  public void initialize() {
    m_finished = false;
    m_winner = null;
    for (Command command : m_commands) {
      command.initialize();
    }
  }

  @Override
  public void execute() {
    for (Command command : m_commands) {
      command.execute();
      if (command.isFinished()) {
        m_finished = true;
        m_winner = command;
      }
    }
  }

  @Override
  public void end(boolean interrupted) {
    for (Command command : m_commands) {
      // The winner finished naturally; all others are interrupted
      command.end(interrupted || command != m_winner);
    }
  }

  @Override
  public boolean isFinished() {
    return m_finished;
  }
}

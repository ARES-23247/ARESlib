package org.areslib.command;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A CommandGroup that runs a set of commands in parallel, ending when any one of the commands ends
 * and interrupting all the others.
 */
public class ParallelRaceGroup extends Command {
  private final Set<Command> commands = new HashSet<>();
  private boolean finished = false;
  private Command winner = null;

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
    if (finished) {
      throw new IllegalStateException(
          "Commands cannot be added to a CommandGroup while the group is running");
    }

    for (Command command : commands) {
      if (!Collections.disjoint(command.getRequirements(), requirements)) {
        throw new IllegalArgumentException(
            "Multiple commands in a parallel group cannot require the same subsystems");
      }
      this.commands.add(command);
      requirements.addAll(command.getRequirements());
    }
  }

  @Override
  public void initialize() {
    finished = false;
    winner = null;
    for (Command command : commands) {
      command.initialize();
    }
  }

  @Override
  public void execute() {
    for (Command command : commands) {
      command.execute();
      if (command.isFinished()) {
        finished = true;
        winner = command;
      }
    }
  }

  @Override
  public void end(boolean interrupted) {
    for (Command command : commands) {
      // The winner finished naturally; all others are interrupted
      command.end(interrupted || command != winner);
    }
  }

  @Override
  public boolean isFinished() {
    return finished;
  }
}

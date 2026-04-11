package org.areslib.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A CommandGroup that runs a set of commands in parallel, ending only when a specific command (the
 * "deadline") ends, interrupting all other commands that are still running at that point.
 */
public class ParallelDeadlineGroup extends Command {
  private final Map<Command, Boolean> commands = new HashMap<>();
  private Command deadline;
  private boolean finished = false;

  /**
   * Creates a new ParallelDeadlineGroup. The given commands will be executed simultaneously. The
   * group will finish when the deadline command finishes.
   *
   * @param deadline The command that determines when the group ends.
   * @param commands The other commands to be executed.
   */
  public ParallelDeadlineGroup(Command deadline, Command... commands) {
    this.deadline = deadline;
    addCommands(deadline);
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
      this.commands.put(command, false);
      requirements.addAll(command.getRequirements());
    }
  }

  /**
   * Sets the deadline to the given command.
   *
   * @param deadline the command that determines when the group ends.
   */
  public void setDeadline(Command deadline) {
    if (!this.commands.containsKey(deadline)) {
      addCommands(deadline);
    }
    this.deadline = deadline;
  }

  @Override
  public void initialize() {
    finished = false;
    for (Map.Entry<Command, Boolean> entry : commands.entrySet()) {
      entry.getKey().initialize();
      entry.setValue(false);
    }
  }

  @Override
  public void execute() {
    for (Map.Entry<Command, Boolean> entry : commands.entrySet()) {
      if (entry.getValue()) {
        continue; // Already finished, skip
      }
      Command command = entry.getKey();
      command.execute();
      if (command.isFinished()) {
        if (command == deadline) {
          finished = true;
        } else {
          // Non-deadline command finished naturally
          command.end(false);
          entry.setValue(true);
        }
      }
    }
  }

  @Override
  public void end(boolean interrupted) {
    for (Map.Entry<Command, Boolean> entry : commands.entrySet()) {
      if (!entry.getValue()) {
        Command command = entry.getKey();
        if (command == deadline) {
          // Deadline finished naturally (or group was interrupted)
          command.end(interrupted);
        } else {
          // Still-running non-deadline commands are always interrupted
          command.end(true);
        }
      }
    }
  }

  @Override
  public boolean isFinished() {
    return finished;
  }
}

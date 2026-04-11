package org.areslib.command;

import java.util.ArrayList;
import java.util.List;

/**
 * A CommandGroup that runs a list of commands in sequence.
 *
 * <p>As a rule, CommandGroups require the union of the requirements of their component commands.
 */
public class SequentialCommandGroup extends Command {
  private final List<Command> commands = new ArrayList<>();
  private int currentCommandIndex = -1;

  /**
   * Creates a new SequentialCommandGroup. The given commands will be run sequentially, with the
   * CommandGroup finishing when the last command finishes.
   *
   * @param commands the commands to include in this group.
   */
  public SequentialCommandGroup(Command... commands) {
    addCommands(commands);
  }

  public final void addCommands(Command... commands) {
    if (currentCommandIndex != -1) {
      throw new IllegalStateException(
          "Commands cannot be added to a CommandGroup while the group is running");
    }

    for (Command command : commands) {
      this.commands.add(command);
      requirements.addAll(command.getRequirements());
      // WPILib requires checking runWhenDisabled, simplifying here
    }
  }

  @Override
  public void initialize() {
    currentCommandIndex = 0;

    if (!commands.isEmpty()) {
      commands.get(0).initialize();
    }
  }

  @Override
  public void execute() {
    if (commands.isEmpty()) {
      return;
    }

    Command currentCommand = commands.get(currentCommandIndex);

    currentCommand.execute();
    if (currentCommand.isFinished()) {
      currentCommand.end(false);
      currentCommandIndex++;
      if (currentCommandIndex < commands.size()) {
        commands.get(currentCommandIndex).initialize();
      }
    }
  }

  @Override
  public void end(boolean interrupted) {
    if (interrupted
        && !commands.isEmpty()
        && currentCommandIndex > -1
        && currentCommandIndex < commands.size()) {
      commands.get(currentCommandIndex).end(true);
    }
    currentCommandIndex = -1;
  }

  @Override
  public boolean isFinished() {
    return currentCommandIndex >= commands.size();
  }
}

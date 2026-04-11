package org.areslib.command;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A command that runs one command from a map of commands based on a key evaluator. Helpful for
 * dynamic autonomous selection.
 *
 * @param <K> The type of the key used to select the command.
 */
public class SelectCommand<K> extends Command {
  private final Map<K, Command> commands;
  private final Supplier<K> selector;
  private Command selectedCommand;

  /**
   * Creates a new SelectCommand.
   *
   * @param commands the map of commands to choose from
   * @param selector the selector that determines which command to run
   */
  public SelectCommand(Map<K, Command> commands, Supplier<K> selector) {
    this.commands = commands;
    this.selector = selector;

    for (Command command : commands.values()) {
      requirements.addAll(command.getRequirements());
    }
  }

  @Override
  public void initialize() {
    K key = selector.get();
    selectedCommand = commands.get(key);

    if (selectedCommand != null) {
      selectedCommand.initialize();
    } else {
      com.qualcomm.robotcore.util.RobotLog.e(
          String.valueOf("SelectCommand: No command found for key: " + key));
    }
  }

  @Override
  public void execute() {
    if (selectedCommand != null) {
      selectedCommand.execute();
    }
  }

  @Override
  public void end(boolean interrupted) {
    if (selectedCommand != null) {
      selectedCommand.end(interrupted);
    }
  }

  @Override
  public boolean isFinished() {
    if (selectedCommand != null) {
      return selectedCommand.isFinished();
    } else {
      return true; // Complete instantly if key is invalid
    }
  }
}

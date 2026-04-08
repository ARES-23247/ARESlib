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
  private final Map<K, Command> m_commands;
  private final Supplier<K> m_selector;
  private Command m_selectedCommand;

  /**
   * Creates a new SelectCommand.
   *
   * @param commands the map of commands to choose from
   * @param selector the selector that determines which command to run
   */
  public SelectCommand(Map<K, Command> commands, Supplier<K> selector) {
    m_commands = commands;
    m_selector = selector;

    for (Command command : m_commands.values()) {
      m_requirements.addAll(command.getRequirements());
    }
  }

  @Override
  public void initialize() {
    K key = m_selector.get();
    m_selectedCommand = m_commands.get(key);

    if (m_selectedCommand != null) {
      m_selectedCommand.initialize();
    } else {
      com.qualcomm.robotcore.util.RobotLog.e(
          String.valueOf("SelectCommand: No command found for key: " + key));
    }
  }

  @Override
  public void execute() {
    if (m_selectedCommand != null) {
      m_selectedCommand.execute();
    }
  }

  @Override
  public void end(boolean interrupted) {
    if (m_selectedCommand != null) {
      m_selectedCommand.end(interrupted);
    }
  }

  @Override
  public boolean isFinished() {
    if (m_selectedCommand != null) {
      return m_selectedCommand.isFinished();
    } else {
      return true; // Complete instantly if key is invalid
    }
  }
}

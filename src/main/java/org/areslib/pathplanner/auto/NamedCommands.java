package org.areslib.pathplanner.auto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.areslib.command.Command;
import org.areslib.command.SequentialCommandGroup;
import org.areslib.math.Pair;
import org.areslib.pathplanner.dummy.DriverStation;

/** Utility class for managing named commands */
public class NamedCommands {
  private static final HashMap<String, Command> NAMED_COMMANDS = new HashMap<>();

  /**
   * Registers a command with the given name.
   *
   * @param name the name of the command
   * @param command the command to register
   */
  public static void registerCommand(String name, Command command) {
    NAMED_COMMANDS.put(name, command);
  }

  /**
   * Registers a list of commands with their associated names.
   *
   * @param commands the list of commands to register
   */
  public static void registerCommands(List<Pair<String, Command>> commands) {
    for (var pair : commands) {
      registerCommand(pair.getFirst(), pair.getSecond());
    }
  }

  /**
   * Registers a map of commands with their associated names.
   *
   * @param commands the map of commands to register
   */
  public static void registerCommands(Map<String, Command> commands) {
    NAMED_COMMANDS.putAll(commands);
  }

  /**
   * Returns whether a command with the given name has been registered.
   *
   * @param name the name of the command to check
   * @return true if a command with the given name has been registered, false otherwise
   */
  public static boolean hasCommand(String name) {
    return NAMED_COMMANDS.containsKey(name);
  }

  /**
   * Returns the command with the given name.
   *
   * @param name the name of the command to get
   * @return the command with the given name, wrapped in a functional command, or a none command if
   *     it has not been registered
   */
  public static Command getCommand(String name) {
    if (hasCommand(name)) {
      return CommandUtil.wrappedEventCommand(NAMED_COMMANDS.get(name));
    } else {
      DriverStation.reportWarning(
          "PathPlanner attempted to create a command '"
              + name
              + "' that has not been registered with NamedCommands.registerCommand",
          false);
      return new SequentialCommandGroup();
    }
  }
}

package org.areslib.hmi;

import java.util.HashMap;
import java.util.Map;
import org.areslib.command.Command;
import org.areslib.command.button.Trigger;
import org.areslib.telemetry.AresTelemetry;

/**
 * A wrapper for gamepad controls that automatically logs button bindings to telemetry.
 *
 * <p>This allows dashboards to visually represent controller bindings and provides runtime
 * visibility into what actions are mapped to which buttons.
 *
 * <p>Essential for driver training and debugging controller configurations.
 */
public class TelemetryGamepad {

  private final String gamepadIdentity;
  private final Map<String, String> currentBindings = new HashMap<>();

  /**
   * Creates a telemetry-enabled gamepad wrapper.
   *
   * @param gamepadIdentity Unique identifier for this gamepad (e.g., "Driver", "Operator")
   */
  public TelemetryGamepad(String gamepadIdentity) {
    this.gamepadIdentity = gamepadIdentity;
  }

  /**
   * Binds a command to a Trigger's `.onTrue()` scheduling method while automatically publishing the
   * mapped action name to telemetry.
   *
   * @param trigger The Trigger instance (e.g., gamepad.a())
   * @param buttonName The human-readable physical button name (e.g., "A_Button")
   * @param actionName The macro the button runs (e.g., "Score Gamepiece")
   * @param command The Command to schedule
   * @return The Trigger to allow method chaining
   */
  public Trigger bindOnTrue(
      Trigger trigger, String buttonName, String actionName, Command command) {
    currentBindings.put(buttonName, actionName);
    AresTelemetry.putString("GamepadBindings/" + gamepadIdentity + "/" + buttonName, actionName);
    return trigger.onTrue(command);
  }

  /**
   * Binds a command to a Trigger's `.whileTrue()` scheduling method while automatically publishing
   * the mapped action name to telemetry.
   *
   * @param trigger The Trigger instance (e.g., gamepad.rightTrigger())
   * @param buttonName The human-readable physical button name (e.g., "RightTrigger")
   * @param actionName The macro the button runs (e.g., "Shoot On Move")
   * @param command The Command to schedule
   * @return The Trigger to allow method chaining
   */
  public Trigger bindWhileTrue(
      Trigger trigger, String buttonName, String actionName, Command command) {
    currentBindings.put(buttonName, actionName);
    AresTelemetry.putString("GamepadBindings/" + gamepadIdentity + "/" + buttonName, actionName);
    return trigger.whileTrue(command);
  }

  /**
   * Binds a command to a Trigger's `.onFalse()` scheduling method while automatically publishing
   * the mapped action name to telemetry.
   *
   * @param trigger The Trigger instance (e.g., gamepad.a())
   * @param buttonName The human-readable physical button name (e.g., "A_Button")
   * @param actionName The macro the button runs (e.g., "Retract System")
   * @param command The Command to schedule
   * @return The Trigger to allow method chaining
   */
  public Trigger bindOnFalse(
      Trigger trigger, String buttonName, String actionName, Command command) {
    // Overwrite standard map as this is likely a dual-trigger
    String releaseAction = actionName + " (Release)";
    currentBindings.put(buttonName, releaseAction);
    AresTelemetry.putString("GamepadBindings/" + gamepadIdentity + "/" + buttonName, releaseAction);
    return trigger.onFalse(command);
  }

  /**
   * Binds a command to toggle on a Trigger.
   *
   * @param trigger The Trigger instance
   * @param buttonName The human-readable physical button name
   * @param actionName The macro the button runs
   * @param command The Command to toggle
   * @return The Trigger to allow method chaining
   */
  public Trigger bindToggle(
      Trigger trigger, String buttonName, String actionName, Command command) {
    currentBindings.put(buttonName, "Toggle: " + actionName);
    AresTelemetry.putString(
        "GamepadBindings/" + gamepadIdentity + "/" + buttonName, "Toggle: " + actionName);
    return trigger.toggleOnTrue(command);
  }

  /**
   * Gets the current button bindings map.
   *
   * @return Map of button names to action names
   */
  public Map<String, String> getBindingsMap() {
    return new HashMap<>(currentBindings);
  }

  /**
   * Gets the gamepad identity string.
   *
   * @return The gamepad identity
   */
  public String getGamepadIdentity() {
    return gamepadIdentity;
  }
}

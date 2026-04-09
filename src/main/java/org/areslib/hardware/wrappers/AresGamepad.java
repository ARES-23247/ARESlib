package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.Gamepad;
import java.util.HashMap;
import java.util.Map;
import org.areslib.command.Command;
import org.areslib.command.button.Trigger;
import org.areslib.telemetry.AresTelemetry;

/**
 * Wrapper around the FTC {@link Gamepad} providing WPILib-style {@link Trigger} bindings and
 * pre-inverted stick axes (Up = positive Y).
 *
 * <p><b>Usage note:</b> Trigger accessors (e.g. {@link #a()}) are lazily cached. It is safe to call
 * {@code gamepad.a().onTrue(cmd)} inside {@code configureBindings()} without creating duplicate
 * registrations.
 */
public class AresGamepad {

  private final Gamepad gamepad;
  private final String name;

  // Cached Trigger instances — lazily initialized on first access
  private Trigger triggerA, triggerB, triggerX, triggerY;
  private Trigger triggerLB, triggerRB;
  private Trigger triggerDU, triggerDD, triggerDL, triggerDR;

  private final Map<String, String> currentBindings = new HashMap<>();

  public AresGamepad(Gamepad gamepad, String name) {
    this.gamepad = gamepad;
    this.name = name;
  }

  public AresGamepad(Gamepad gamepad) {
    this(gamepad, "Gamepad");
  }

  private void recordBinding(String button, String actionDescription) {
    AresTelemetry.putString("ControllerMap/" + name + "/" + button, actionDescription);
  }

  public double getLeftX() {
    return gamepad.left_stick_x;
  }

  public double getLeftY() {
    return -gamepad.left_stick_y;
  } // Invert so Up is positive natively

  public double getRightX() {
    return gamepad.right_stick_x;
  }

  public double getRightY() {
    return -gamepad.right_stick_y;
  }

  public double getLeftTriggerAxis() {
    return gamepad.left_trigger;
  }

  public double getRightTriggerAxis() {
    return gamepad.right_trigger;
  }

  // WPILib-style Trigger Bindings (lazily cached to prevent duplicate registrations)
  public Trigger a() {
    if (triggerA == null) triggerA = new Trigger(() -> gamepad.a);
    return triggerA;
  }

  public Trigger a(String actionDescription) {
    recordBinding("A", actionDescription);
    return a();
  }

  public Trigger b() {
    if (triggerB == null) triggerB = new Trigger(() -> gamepad.b);
    return triggerB;
  }

  public Trigger b(String actionDescription) {
    recordBinding("B", actionDescription);
    return b();
  }

  public Trigger x() {
    if (triggerX == null) triggerX = new Trigger(() -> gamepad.x);
    return triggerX;
  }

  public Trigger x(String actionDescription) {
    recordBinding("X", actionDescription);
    return x();
  }

  public Trigger y() {
    if (triggerY == null) triggerY = new Trigger(() -> gamepad.y);
    return triggerY;
  }

  public Trigger y(String actionDescription) {
    recordBinding("Y", actionDescription);
    return y();
  }

  public Trigger leftBumper() {
    if (triggerLB == null) triggerLB = new Trigger(() -> gamepad.left_bumper);
    return triggerLB;
  }

  public Trigger leftBumper(String actionDescription) {
    recordBinding("Left Bumper", actionDescription);
    return leftBumper();
  }

  public Trigger rightBumper() {
    if (triggerRB == null) triggerRB = new Trigger(() -> gamepad.right_bumper);
    return triggerRB;
  }

  public Trigger rightBumper(String actionDescription) {
    recordBinding("Right Bumper", actionDescription);
    return rightBumper();
  }

  public Trigger dpadUp() {
    if (triggerDU == null) triggerDU = new Trigger(() -> gamepad.dpad_up);
    return triggerDU;
  }

  public Trigger dpadUp(String actionDescription) {
    recordBinding("D-Pad Up", actionDescription);
    return dpadUp();
  }

  public Trigger dpadDown() {
    if (triggerDD == null) triggerDD = new Trigger(() -> gamepad.dpad_down);
    return triggerDD;
  }

  public Trigger dpadDown(String actionDescription) {
    recordBinding("D-Pad Down", actionDescription);
    return dpadDown();
  }

  public Trigger dpadLeft() {
    if (triggerDL == null) triggerDL = new Trigger(() -> gamepad.dpad_left);
    return triggerDL;
  }

  public Trigger dpadLeft(String actionDescription) {
    recordBinding("D-Pad Left", actionDescription);
    return dpadLeft();
  }

  public Trigger dpadRight() {
    if (triggerDR == null) triggerDR = new Trigger(() -> gamepad.dpad_right);
    return triggerDR;
  }

  public Trigger dpadRight(String actionDescription) {
    recordBinding("D-Pad Right", actionDescription);
    return dpadRight();
  }

  /**
   * Binds a command to a Trigger's `.onTrue()` scheduling method while automatically publishing the
   * mapped action name to AresTelemetry.
   *
   * @param trigger The Trigger instance (e.g. this.a())
   * @param buttonName The human-readable physical button name (e.g. "A_Button")
   * @param actionName The macro the button runs (e.g. "Score Gamepiece")
   * @param cmd The Command to schedule
   * @return The Trigger to allow method chaining
   */
  public Trigger bindOnTrue(Trigger trigger, String buttonName, String actionName, Command cmd) {
    currentBindings.put(buttonName, actionName);
    AresTelemetry.putString("GamepadBindings/" + name + "/" + buttonName, actionName);
    return trigger.onTrue(cmd);
  }

  /**
   * Binds a command to a Trigger's `.whileTrue()` scheduling method while automatically publishing
   * the mapped action name to AresTelemetry.
   *
   * @param trigger The Trigger instance (e.g. this.a())
   * @param buttonName The human-readable physical button name (e.g. "RightTrigger")
   * @param actionName The macro the button runs (e.g. "Shoot On Move")
   * @param cmd The Command to schedule
   * @return The Trigger to allow method chaining
   */
  public Trigger bindWhileTrue(Trigger trigger, String buttonName, String actionName, Command cmd) {
    currentBindings.put(buttonName, actionName);
    AresTelemetry.putString("GamepadBindings/" + name + "/" + buttonName, actionName);
    return trigger.whileTrue(cmd);
  }

  /**
   * Binds a command to a Trigger's `.onFalse()` scheduling method while automatically publishing
   * the mapped action name to AresTelemetry.
   *
   * @param trigger The Trigger instance (e.g. this.a())
   * @param buttonName The human-readable physical button name (e.g. "A_Button")
   * @param actionName The macro the button runs (e.g. "Retract System")
   * @param cmd The Command to schedule
   * @return The Trigger to allow method chaining
   */
  public Trigger bindOnFalse(Trigger trigger, String buttonName, String actionName, Command cmd) {
    currentBindings.put(buttonName, actionName + " (Release)");
    AresTelemetry.putString(
        "GamepadBindings/" + name + "/" + buttonName, actionName + " (Release)");
    return trigger.onFalse(cmd);
  }

  /**
   * Generates a raw mapping of binding pairs.
   *
   * @return A map mapping physical button names to current string macros.
   */
  public Map<String, String> getBindingsMap() {
    return currentBindings;
  }

  /**
   * Returns the raw underlying FTC Gamepad object.
   *
   * @return The raw Gamepad object.
   */
  public Gamepad getGamepad() {
    return gamepad;
  }

  /**
   * Rumbles the controller.
   *
   * @param durationMs Duration in milliseconds.
   */
  public void rumble(int durationMs) {
    if (gamepad != null) {
      gamepad.rumble(durationMs);
    }
  }

  /**
   * Rumbles the controller with variable strength.
   *
   * @param rumble1 Motor 1 strength (0.0 to 1.0)
   * @param rumble2 Motor 2 strength (0.0 to 1.0)
   * @param durationMs Duration in milliseconds.
   */
  public void rumble(double rumble1, double rumble2, int durationMs) {
    if (gamepad != null) {
      gamepad.rumble(rumble1, rumble2, durationMs);
    }
  }

  /**
   * Sets the LED color of the controller (PS4/PS5 compatibility).
   *
   * @param r Red (0.0 to 1.0)
   * @param g Green (0.0 to 1.0)
   * @param b Blue (0.0 to 1.0)
   * @param durationMs Duration in ms.
   */
  public void setLedColor(double r, double g, double b, int durationMs) {
    if (gamepad != null) {
      gamepad.setLedColor(r, g, b, durationMs);
    }
  }
}

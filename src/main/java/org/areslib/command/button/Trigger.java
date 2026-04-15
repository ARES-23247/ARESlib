package org.areslib.command.button;

import java.util.function.BooleanSupplier;
import org.areslib.command.Command;
import org.areslib.command.CommandScheduler;

/**
 * A class used to bind command execution to a boolean condition (such as a gamepad button press).
 *
 * <p>Triggers continuously evaluate their boolean condition and can schedule or cancel commands in
 * the {@link CommandScheduler} when their state changes.
 */
public class Trigger {

  private final BooleanSupplier condition;

  /**
   * Constructs a new Trigger based on a boolean condition.
   *
   * @param condition The condition that determines the active state of the trigger.
   */
  public Trigger(BooleanSupplier condition) {
    this.condition = condition;
  }

  /**
   * Retrieves the current state of the trigger condition.
   *
   * @return True if the condition is met, false otherwise.
   */
  public boolean getAsBoolean() {
    return condition.getAsBoolean();
  }

  /**
   * Starts the given command whenever the trigger just becomes active.
   *
   * <p>The command is scheduled on the rising edge of the condition (i.e. changing from false to
   * true).
   *
   * @param command The command to start.
   * @return This trigger, for builder-style method chaining.
   */
  public Trigger onTrue(Command command) {
    // Each binding gets its own isolated state tracker to prevent cross-contamination
    final boolean[] prev = {false};
    CommandScheduler.getInstance()
        .addButton(
            () -> {
              boolean currentState = getAsBoolean();
              if (currentState && !prev[0]) {
                CommandScheduler.getInstance().schedule(command);
              }
              prev[0] = currentState;
            });
    return this;
  }

  /**
   * Starts the given command when the trigger initially becomes active, and cancels it when it
   * becomes inactive.
   *
   * <p>The command is scheduled on the rising edge and cancelled on the falling edge of the
   * condition.
   *
   * @param command The command to start and cancel.
   * @return This trigger, for builder-style method chaining.
   */
  public Trigger whileTrue(Command command) {
    final boolean[] prev = {false};
    CommandScheduler.getInstance()
        .addButton(
            () -> {
              boolean currentState = getAsBoolean();
              if (currentState && !prev[0]) {
                CommandScheduler.getInstance().schedule(command);
              } else if (!currentState && prev[0]) {
                CommandScheduler.getInstance().cancel(command);
              }
              prev[0] = currentState;
            });
    return this;
  }

  /**
   * Starts the given command whenever the trigger just becomes inactive.
   *
   * <p>The command is scheduled on the falling edge of the condition (i.e. changing from true to
   * false).
   *
   * @param command The command to start.
   * @return This trigger, for builder-style method chaining.
   */
  public Trigger onFalse(Command command) {
    final boolean[] prev = {false};
    CommandScheduler.getInstance()
        .addButton(
            () -> {
              boolean currentState = getAsBoolean();
              if (!currentState && prev[0]) {
                CommandScheduler.getInstance().schedule(command);
              }
              prev[0] = currentState;
            });
    return this;
  }

  /**
   * Toggles a command on and off each time the trigger becomes active.
   *
   * <p>On the first rising edge the command is scheduled. On the next rising edge it is cancelled.
   * This is commonly used for toggle mechanisms like grippers or intakes.
   *
   * @param command The command to toggle.
   * @return This trigger, for builder-style method chaining.
   */
  public Trigger toggleOnTrue(Command command) {
    final boolean[] prev = {false};
    final boolean[] toggled = {false};
    CommandScheduler.getInstance()
        .addButton(
            () -> {
              boolean currentState = getAsBoolean();
              if (currentState && !prev[0]) {
                if (toggled[0]) {
                  CommandScheduler.getInstance().cancel(command);
                } else {
                  CommandScheduler.getInstance().schedule(command);
                }
                toggled[0] = !toggled[0];
              }
              prev[0] = currentState;
            });
    return this;
  }

  /**
   * Composes two triggers with logical AND.
   *
   * @param trigger the condition to compose with
   * @return the composed trigger
   */
  public Trigger and(BooleanSupplier trigger) {
    return new Trigger(() -> condition.getAsBoolean() && trigger.getAsBoolean());
  }

  /**
   * Composes two triggers with logical OR.
   *
   * @param trigger the condition to compose with
   * @return the composed trigger
   */
  public Trigger or(BooleanSupplier trigger) {
    return new Trigger(() -> condition.getAsBoolean() || trigger.getAsBoolean());
  }

  /**
   * Creates a new trigger that is active when this trigger is inactive.
   *
   * @return the negated trigger
   */
  public Trigger negate() {
    return new Trigger(() -> !condition.getAsBoolean());
  }

  /**
   * Creates a new trigger that only activates after the condition has been continuously true for
   * the specified duration. Resets if the condition becomes false.
   *
   * <p>Uses precise elapsed time measurement based on {@link org.areslib.core.AresTimer} rather
   * than deterministic loop counting to ensure consistent behavior.
   *
   * @param seconds The duration the condition must be continuously true before activating.
   * @return The debounced trigger.
   */
  public Trigger debounce(double seconds) {
    final double[] elapsed = {0.0};
    final double[] lastTime = {org.areslib.core.AresTimer.getFPGATimestamp()};
    return new Trigger(
        () -> {
          double currentTime = org.areslib.core.AresTimer.getFPGATimestamp();
          if (condition.getAsBoolean()) {
            elapsed[0] += (currentTime - lastTime[0]);
          } else {
            elapsed[0] = 0.0;
          }
          lastTime[0] = currentTime;
          return elapsed[0] >= seconds;
        });
  }
}

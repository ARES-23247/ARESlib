package org.areslib.subsystems.controllers.examples;

import org.areslib.core.StateMachine;
import org.areslib.telemetry.AresAutoLogger;

/**
 * EXAMPLE: Manages robot operational modes using state machine pattern.
 *
 * <p>This is an EXAMPLE demonstrating how to use StateMachine for controller mode management. Adapt
 * this pattern to your specific game and robot requirements.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ControllerModeManagerExample modes = new ControllerModeManagerExample();
 * modes.configure();
 *
 * // In periodic loop:
 * modes.update();
 *
 * // Mode-specific behavior:
 * if (modes.getMode() == ControllerModeExample.SPEAKER) {
 *   shooter.enableHighSpeed();
 * }
 * }</pre>
 */
public class ControllerModeManagerExample {

  private final StateMachine<ControllerModeExample> stateMachine;
  private double modeSwitchTime = 0.0;

  /** Constructs a new ControllerModeManagerExample starting in IDLE mode. */
  public ControllerModeManagerExample() {
    stateMachine = new StateMachine<>(ControllerModeExample.IDLE);
  }

  /**
   * Configures all state machine transitions, entry/exit actions, and mode-specific behaviors.
   *
   * <p>Call this after construction to set up the complete mode hierarchy.
   */
  public void configure() {
    // Entry actions for each mode
    stateMachine.onEntry(
        ControllerModeExample.SPEAKER,
        () -> {
          logModeChange("SPEAKER");
          // Enable high-speed shooter, vision targeting to speaker
        });

    stateMachine.onEntry(
        ControllerModeExample.HP,
        () -> {
          logModeChange("HP");
          // Extend intake, prepare for gentle handling
        });

    stateMachine.onEntry(
        ControllerModeExample.POOP,
        () -> {
          logModeChange("POOP");
          // Lower intake for ground pickup
        });

    stateMachine.onEntry(
        ControllerModeExample.CLIMB,
        () -> {
          logModeChange("CLIMB");
          // Disable shooter, engage climb brakes
        });

    stateMachine.onEntry(
        ControllerModeExample.AMP,
        () -> {
          logModeChange("AMP");
          // Configure for lob shots or direct placement
        });

    stateMachine.onEntry(
        ControllerModeExample.AUTO,
        () -> {
          logModeChange("AUTO");
          // Disable manual controls, enable path following
        });

    // Exit actions to clean up mode-specific state
    stateMachine.onExit(
        ControllerModeExample.SPEAKER,
        () -> {
          // Disable vision targeting
        });

    stateMachine.onExit(
        ControllerModeExample.CLIMB,
        () -> {
          // Re-enable scoring systems
        });

    // Conditional transitions (can be driven by gamepad, field state, or auto decisions)
    stateMachine.transition(
        ControllerModeExample.HP,
        ControllerModeExample.SPEAKER,
        () -> {
          // Condition: Has game piece + ready to shoot
          return hasGamePiece() && isReadyToShoot();
        });

    // Auto-transition from CLIMB to IDLE after climb complete (with timeout safety)
    stateMachine.transitionAfter(ControllerModeExample.CLIMB, ControllerModeExample.IDLE, 15.0);
  }

  /** Updates the state machine. Must be called every periodic loop. */
  public void update() {
    stateMachine.update();
  }

  /**
   * Forces a transition to the specified mode.
   *
   * @param mode The target mode.
   */
  public void setMode(ControllerModeExample mode) {
    if (stateMachine.getState() != mode) {
      modeSwitchTime = getCurrentTime();
      stateMachine.forceState(mode);
    }
  }

  /**
   * Returns the current active mode.
   *
   * @return The current controller mode.
   */
  public ControllerModeExample getMode() {
    return stateMachine.getState();
  }

  /**
   * Returns the previous mode.
   *
   * @return The previous controller mode.
   */
  public ControllerModeExample getPreviousMode() {
    return stateMachine.getPreviousState();
  }

  /**
   * Returns time spent in current mode (seconds).
   *
   * @return Time in current mode.
   */
  public double getTimeInMode() {
    return stateMachine.getTimeInState();
  }

  /**
   * Returns time since last mode switch (seconds).
   *
   * @return Time since mode switch.
   */
  public double getTimeSinceModeSwitch() {
    return getCurrentTime() - modeSwitchTime;
  }

  /**
   * Checks if robot is in a specific mode.
   *
   * @param mode The mode to check.
   * @return True if currently in the specified mode.
   */
  public boolean isInMode(ControllerModeExample mode) {
    return stateMachine.isInState(mode);
  }

  /**
   * Checks if robot is in any scoring mode (SPEAKER, AMP).
   *
   * @return True if in a scoring mode.
   */
  public boolean isScoringMode() {
    ControllerModeExample mode = getMode();
    return mode == ControllerModeExample.SPEAKER || mode == ControllerModeExample.AMP;
  }

  /**
   * Checks if robot is in any intake mode (HP, POOP).
   *
   * @return True if in an intake mode.
   */
  public boolean isIntakeMode() {
    ControllerModeExample mode = getMode();
    return mode == ControllerModeExample.HP || mode == ControllerModeExample.POOP;
  }

  // ── Helper conditions for transitions ────────────────────────────────────────

  private boolean hasGamePiece() {
    // TODO: Integrate with actual game piece sensor
    return false;
  }

  private boolean isReadyToShoot() {
    // TODO: Check shooter readiness, target lock, etc.
    return false;
  }

  private void logModeChange(String mode) {
    AresAutoLogger.recordOutput("ControllerMode/CurrentMode", mode);
    AresAutoLogger.recordOutput("ControllerMode/LastSwitchTime", getCurrentTime());
  }

  private static double getCurrentTime() {
    return System.nanoTime() / 1_000_000_000.0;
  }
}

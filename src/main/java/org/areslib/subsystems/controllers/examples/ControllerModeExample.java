package org.areslib.subsystems.controllers.examples;

/**
 * EXAMPLE: Operational modes for robot control, inspired by elite FRC team architectures.
 *
 * <p>This is an EXAMPLE demonstrating how to structure robot operational modes. Adapt this to your
 * specific game and robot requirements.
 *
 * <p>Based on Team 254 (Cheesy Poofs) 2024 controller mode architecture.
 */
public enum ControllerModeExample {
  /** Default mode with no active scoring behavior. */
  IDLE,

  /** High-speed scoring into the speaker/primary goal. */
  SPEAKER,

  /** Human Player intake mode. */
  HP,

  /** Ground intake mode ("POOP" - picking up from floor). */
  POOP,

  /** Endgame climbing mode. */
  CLIMB,

  /** Amp/side-scoring mode. */
  AMP,

  /** Autonomous mode. */
  AUTO
}

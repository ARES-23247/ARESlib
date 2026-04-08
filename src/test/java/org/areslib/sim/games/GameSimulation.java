package org.areslib.sim.games;

import java.util.List;
import org.dyn4j.dynamics.Body;
import org.dyn4j.world.World;

/**
 * Architecture for simulated FTC games. Decouples the physical game layout (walls, samples,
 * kinematics) from the pure robot engine loop.
 */
public interface GameSimulation {

  /**
   * Initializes the field. Expected to construct field walls and spawn game pieces into the
   * physicsWorld.
   */
  void initField(World<Body> physicsWorld);

  /**
   * 50Hz periodic processing of robot interactions with the game world. Evaluates collision
   * thresholds (e.g., intaking a sample) mapped explicitly to the array of robots on the field.
   */
  void updateField(World<Body> physicsWorld, List<RobotSimState> robots);

  /**
   * Pushes the states of all static/movable game pieces to the AdvantageScope telemetry backend.
   */
  void telemetryUpdate();

  /** Gets the number of held game pieces for a specific robot. */
  int getHeldSamples(RobotSimState robot);
}

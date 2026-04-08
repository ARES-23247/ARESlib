package org.areslib.core.simulation;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.world.World;

/**
 * Singleton wrapper around the Dyn4j Physics World to manage physical bounds, robots, and DECODE
 * game elements logically separated from internal controllers.
 */
public class AresPhysicsWorld {

  private static AresPhysicsWorld instance;
  private final World<Body> world;

  private AresPhysicsWorld() {
    this.world = new World<>();
    // No gravity; top-down 2D simulation
    this.world.setGravity(World.ZERO_GRAVITY);

    Settings settings = new Settings();
    // Adjust for typical robot constraints
    settings.setMaximumTranslation(2.0);
    this.world.setSettings(settings);
  }

  public static synchronized AresPhysicsWorld getInstance() {
    if (instance == null) {
      instance = new AresPhysicsWorld();
    }
    return instance;
  }

  public World<Body> getWorld() {
    return world;
  }

  /**
   * Steps the physical simulation by a discrete timestep
   *
   * @param dtSeconds Timestep in seconds
   */
  public synchronized void step(double dtSeconds) {
    if (world != null) {
      world.update(dtSeconds);
    }
  }

  /**
   * Registers a new physics body into the shared environment.
   *
   * @param body The cleanly constructed dyn4j Body
   */
  public synchronized void addBody(Body body) {
    if (!world.containsBody(body)) {
      world.addBody(body);
    }
  }

  /**
   * Flushes the current physics simulation state, usually triggered during robotInit or resetting.
   */
  public synchronized void reset() {
    world.removeAllBodies();
  }
}

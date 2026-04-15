package org.areslib.core.simulation;

import java.util.HashMap;
import java.util.Map;
import org.areslib.telemetry.AresTelemetry;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.world.World;

/**
 * Enhanced physics world manager for ARESLib with battery simulation and telemetry export.
 *
 * <p>This extends the basic AresPhysicsWorld with MARSLib-style features including:
 *
 * <ul>
 *   <li>Battery voltage sag simulation based on current draw
 *   <li>Body pose export to telemetry for 3D visualization
 *   <li>Frame current draw tracking
 *   <li>Named mechanism body management
 *   <li>Stale state detection
 * </ul>
 */
public class EnhancedAresPhysicsWorld {

  private static EnhancedAresPhysicsWorld instance;
  private static int accessCountSinceReset = 0;

  /** Maximum getInstance() calls before a stale-state warning is logged. */
  private static final int STALE_ACCESS_THRESHOLD = 500;

  private static final int STALE_BODY_THRESHOLD = 20;

  private final World<Body> world;
  private final Map<String, Body> mechanismBodies;

  private double frameCurrentDrawAmps = 0.0;
  private double simulatedVoltage = 12.0;
  private double nominalVoltage = 12.0;
  private double internalResistance = 0.02; // Ohms, typical for FTC battery

  private EnhancedAresPhysicsWorld() {
    this.world = new World<>();
    this.world.setGravity(World.ZERO_GRAVITY);

    Settings settings = new Settings();
    settings.setMaximumTranslation(2.0);
    this.world.setSettings(settings);

    this.mechanismBodies = new HashMap<>();
  }

  /**
   * Gets the singleton instance with stale state detection.
   *
   * @return The physics world instance
   */
  public static synchronized EnhancedAresPhysicsWorld getInstance() {
    if (instance == null) {
      instance = new EnhancedAresPhysicsWorld();
      accessCountSinceReset = 0;
    }
    accessCountSinceReset++;

    if (accessCountSinceReset == STALE_ACCESS_THRESHOLD
        && instance.getBodyCount() > STALE_BODY_THRESHOLD) {
      AresTelemetry.putString(
          "PhysicsSim/StaleWarning",
          String.format(
              "WARNING: EnhancedAresPhysicsWorld has %d bodies after %d accesses without reset."
                  + " Did you forget EnhancedAresPhysicsWorld.resetInstance() in @BeforeEach?",
              instance.getBodyCount(), accessCountSinceReset));
    }
    return instance;
  }

  /** Resets the singleton instance. Call this in test setup or OpMode init. */
  @SuppressWarnings("PMD.NullAssignment")
  public static void resetInstance() {
    instance = null;
    accessCountSinceReset = 0;
  }

  public World<Body> getWorld() {
    return world;
  }

  /**
   * Steps the physics simulation and updates battery voltage.
   *
   * @param dtSeconds Timestep in seconds
   */
  public synchronized void update(double dtSeconds) {
    if (world != null) {
      world.update(dtSeconds);
    }

    // Compute battery voltage sag: V = V_nominal - I * R
    double voltageDrop = frameCurrentDrawAmps * internalResistance;
    simulatedVoltage = nominalVoltage - voltageDrop;
    simulatedVoltage = Math.max(6.0, simulatedVoltage); // Don't go below 6V

    // Log telemetry
    AresTelemetry.putNumber("PhysicsWorld/FrameCurrentDraw_A", frameCurrentDrawAmps);
    AresTelemetry.putNumber("PhysicsWorld/ComputedVoltage", simulatedVoltage);
    AresTelemetry.putNumber("PhysicsWorld/BodyCount", getBodyCount());

    // Reset current draw for next frame
    frameCurrentDrawAmps = 0.0;

    // Export body poses to telemetry
    exportToTelemetry();
  }

  /**
   * Registers a named mechanism body for tracking and export.
   *
   * @param name Unique name for this mechanism
   * @param body The physics body
   */
  public synchronized void registerMechanismBody(String name, Body body) {
    mechanismBodies.put(name, body);
    if (!world.containsBody(body)) {
      world.addBody(body);
    }
  }

  /**
   * Adds current draw to the frame total for battery sag calculation.
   *
   * @param amps Current draw in amps
   */
  public void addFrameCurrentDrawAmps(double amps) {
    frameCurrentDrawAmps += amps;
  }

  /**
   * Gets the current simulated battery voltage.
   *
   * @return Battery voltage in volts
   */
  public double getSimulatedVoltage() {
    return simulatedVoltage;
  }

  /**
   * Sets the nominal battery voltage (default 12.0V).
   *
   * @param volts Nominal voltage
   */
  public void setNominalVoltage(double volts) {
    this.nominalVoltage = volts;
  }

  /**
   * Sets the battery internal resistance for sag calculation (default 0.02 Ohms).
   *
   * @param ohms Internal resistance in ohms
   */
  public void setInternalResistance(double ohms) {
    this.internalResistance = ohms;
  }

  /**
   * Gets the number of bodies in the physics world.
   *
   * @return Body count
   */
  public int getBodyCount() {
    return world.getBodyCount();
  }

  /** Exports mechanism body poses to telemetry for 3D visualization. */
  private void exportToTelemetry() {
    for (Map.Entry<String, Body> entry : mechanismBodies.entrySet()) {
      String mechanismName = entry.getKey();
      Body body = entry.getValue();

      double xMeters = body.getTransform().getTranslationX();
      double yMeters = body.getTransform().getTranslationY();
      double yawRads = body.getTransform().getRotationAngle();

      // Export as separate telemetry fields
      String base = "PhysicsWorld/" + mechanismName;
      AresTelemetry.putNumber(base + "/X", xMeters);
      AresTelemetry.putNumber(base + "/Y", yMeters);
      AresTelemetry.putNumber(base + "/Yaw", yawRads);
    }
  }

  /**
   * Adds a body to the physics world.
   *
   * @param body The body to add
   */
  public synchronized void addBody(Body body) {
    if (!world.containsBody(body)) {
      world.addBody(body);
    }
  }

  /**
   * Removes a body from the physics world.
   *
   * @param body The body to remove
   */
  public synchronized void removeBody(Body body) {
    if (world.containsBody(body)) {
      world.removeBody(body);
    }
    mechanismBodies.values().remove(body);
  }

  /** Resets the physics world, removing all bodies and listeners. */
  public synchronized void reset() {
    world.removeAllBodies();
    world.removeAllListeners();
    mechanismBodies.clear();
    frameCurrentDrawAmps = 0.0;
    simulatedVoltage = nominalVoltage;
  }

  /**
   * Adds a contact listener to the physics world.
   *
   * @param listener The contact listener to add
   */
  public synchronized void addContactListener(
      org.dyn4j.world.listener.ContactListener<Body> listener) {
    world.addContactListener(listener);
  }

  /**
   * Gets a named mechanism body.
   *
   * @param name The mechanism name
   * @return The body, or null if not found
   */
  public Body getMechanismBody(String name) {
    return mechanismBodies.get(name);
  }
}

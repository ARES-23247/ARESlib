package org.areslib.core.simulation;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;

/**
 * Procedural generation of the DECODE (2025-2026 FTC Game) field elements. Rebuilds the Dyn4j
 * spatial environment for simulation testing.
 */
public class DecodeFieldSim {

  // 5-inch diameter wiffle-ball "Artifacts". 5 inches = 0.127 meters exactly
  public static final double ARTIFACT_RADIUS_METERS = 0.127 / 2.0;

  /**
   * Initializes the DECODE field constraints, bounds, and game objects into the shared physics
   * world.
   *
   * <p><b>Ordering Contract:</b> This method calls {@link AresPhysicsWorld#reset()}, which destroys
   * ALL existing bodies and listeners. Subsystems that register their own physics bodies (e.g.,
   * chassis, mechanism colliders) <b>must</b> do so <b>after</b> this method returns. {@code
   * AresSimulator.startPhysicsSim()} enforces this ordering automatically.
   */
  public static void buildField() {
    AresPhysicsWorld worldWrapper = AresPhysicsWorld.getInstance();
    worldWrapper.reset(); // Clear old instances

    createFieldBounds(worldWrapper);
    createClassifiers(worldWrapper);

    // Spawn default artifact layouts
    spawnArtifacts(worldWrapper);
  }

  private static void createFieldBounds(AresPhysicsWorld worldWrapper) {
    // Standard FTC field is 144" (3.6576m).
    double halfWidth = 1.8288;

    Body wallTop = new Body();
    wallTop.addFixture(Geometry.createRectangle(3.6576, 0.1));
    wallTop.translate(0, halfWidth + 0.05);
    wallTop.setMass(MassType.INFINITE);
    worldWrapper.addBody(wallTop);

    Body wallBottom = new Body();
    wallBottom.addFixture(Geometry.createRectangle(3.6576, 0.1));
    wallBottom.translate(0, -(halfWidth + 0.05));
    wallBottom.setMass(MassType.INFINITE);
    worldWrapper.addBody(wallBottom);

    Body wallLeft = new Body();
    wallLeft.addFixture(Geometry.createRectangle(0.1, 3.6576));
    wallLeft.translate(-(halfWidth + 0.05), 0);
    wallLeft.setMass(MassType.INFINITE);
    worldWrapper.addBody(wallLeft);

    Body wallRight = new Body();
    wallRight.addFixture(Geometry.createRectangle(0.1, 3.6576));
    wallRight.translate((halfWidth + 0.05), 0);
    wallRight.setMass(MassType.INFINITE);
    worldWrapper.addBody(wallRight);
  }

  private static void createClassifiers(AresPhysicsWorld worldWrapper) {
    // Static Game Elements: Goals (0.6858 x 0.6858)
    Body redGoal = new Body();
    redGoal.addFixture(Geometry.createRectangle(0.6858, 0.6858));
    redGoal.translate(-1.4859, -0.9144);
    redGoal.setMass(MassType.INFINITE);
    worldWrapper.addBody(redGoal);

    Body blueGoal = new Body();
    blueGoal.addFixture(Geometry.createRectangle(0.6858, 0.6858));
    blueGoal.translate(-1.4859, 0.9144);
    blueGoal.setMass(MassType.INFINITE);
    worldWrapper.addBody(blueGoal);

    // Static Game Elements: Loading Zones (0.5842 x 0.5842)
    Body redLoadingZone = new Body();
    redLoadingZone.addFixture(Geometry.createRectangle(0.5842, 0.5842));
    redLoadingZone.translate(1.5367, -1.5367);
    redLoadingZone.setMass(MassType.INFINITE);
    worldWrapper.addBody(redLoadingZone);

    Body blueLoadingZone = new Body();
    blueLoadingZone.addFixture(Geometry.createRectangle(0.5842, 0.5842));
    blueLoadingZone.translate(1.5367, 1.5367);
    blueLoadingZone.setMass(MassType.INFINITE);
    worldWrapper.addBody(blueLoadingZone);
  }

  /** Helper to spawn individual 5-inch artifacts. */
  private static void spawnArtifact(AresPhysicsWorld worldWrapper, double xMeters, double yMeters) {
    Body artifact = new Body();
    BodyFixture fixture = artifact.addFixture(Geometry.createCircle(ARTIFACT_RADIUS_METERS));

    // Polypropylene balls are light and bouncy
    fixture.setDensity(50.0); // kg/m^3 approximation
    fixture.setFriction(0.3); // moderate floor friction
    fixture.setRestitution(0.6); // high bounce (wiffle ball physics)

    artifact.translate(xMeters, yMeters);
    artifact.setMass(MassType.NORMAL);

    // Tag the body so Intakes know this is a game piece
    artifact.setUserData("DECODE_ARTIFACT");

    // Linear/angular damping to simulate friction stopping balls from rolling forever
    artifact.setLinearDamping(1.5);
    artifact.setAngularDamping(1.5);

    worldWrapper.addBody(artifact);
  }

  private static void spawnArtifactCluster(
      AresPhysicsWorld worldWrapper, double xCenter, double yCenter) {
    // Spawn 3 artifacts slightly staggered to prevent physics engine overlap explosion
    spawnArtifact(worldWrapper, xCenter, yCenter + 0.05);
    spawnArtifact(worldWrapper, xCenter + 0.04, yCenter - 0.03);
    spawnArtifact(worldWrapper, xCenter - 0.04, yCenter - 0.03);
  }

  private static void spawnArtifacts(AresPhysicsWorld worldWrapper) {
    // Red Alliance Artifact Spawns (Y = -0.9144 m)
    spawnArtifactCluster(worldWrapper, 0.6096, -0.9144); // Near Spike Mark
    spawnArtifactCluster(worldWrapper, 0.0000, -0.9144); // Middle Spike Mark
    spawnArtifactCluster(worldWrapper, -0.6096, -0.9144); // Far Spike Mark
    spawnArtifactCluster(worldWrapper, 1.68, -1.68); // Loading Zone Staging

    // Blue Alliance Artifact Spawns (Y = +0.9144 m)
    spawnArtifactCluster(worldWrapper, 0.6096, 0.9144); // Near Spike Mark
    spawnArtifactCluster(worldWrapper, 0.0000, 0.9144); // Middle Spike Mark
    spawnArtifactCluster(worldWrapper, -0.6096, 0.9144); // Far Spike Mark
    spawnArtifactCluster(worldWrapper, 1.68, 1.68); // Loading Zone Staging
  }
}

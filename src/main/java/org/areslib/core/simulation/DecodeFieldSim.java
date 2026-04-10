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
    // Standard FTC field is roughly 3.66m x 3.66m (12'x12').
    double halfWidth = 3.66 / 2.0;

    Body wallTop = new Body();
    wallTop.addFixture(Geometry.createRectangle(3.66, 0.1));
    wallTop.translate(0, halfWidth + 0.05);
    wallTop.setMass(MassType.INFINITE);
    worldWrapper.addBody(wallTop);

    Body wallBottom = new Body();
    wallBottom.addFixture(Geometry.createRectangle(3.66, 0.1));
    wallBottom.translate(0, -(halfWidth + 0.05));
    wallBottom.setMass(MassType.INFINITE);
    worldWrapper.addBody(wallBottom);

    Body wallLeft = new Body();
    wallLeft.addFixture(Geometry.createRectangle(0.1, 3.66));
    wallLeft.translate(-(halfWidth + 0.05), 0);
    wallLeft.setMass(MassType.INFINITE);
    worldWrapper.addBody(wallLeft);

    Body wallRight = new Body();
    wallRight.addFixture(Geometry.createRectangle(0.1, 3.66));
    wallRight.translate((halfWidth + 0.05), 0);
    wallRight.setMass(MassType.INFINITE);
    worldWrapper.addBody(wallRight);
  }

  // Obelisk is outside the active field bounds, so no physics body is needed.
  private static void createClassifiers(AresPhysicsWorld worldWrapper) {
    // Goals (Ramp + Gate) placed generally on the sides or corners.
    // We will represent them as static geometric bounds.
    // Red Classifier (Right side approximation)
    Body redClassifier = new Body();
    redClassifier.addFixture(Geometry.createRectangle(0.6, 0.6));
    redClassifier.translate(1.5, 1.5);
    redClassifier.setMass(MassType.INFINITE);
    worldWrapper.addBody(redClassifier);

    // Blue Classifier (Left side approximation)
    Body blueClassifier = new Body();
    blueClassifier.addFixture(Geometry.createRectangle(0.6, 0.6));
    blueClassifier.translate(-1.5, -1.5);
    blueClassifier.setMass(MassType.INFINITE);
    worldWrapper.addBody(blueClassifier);
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

    // Linear/angular damping to simulate friction stopping balls from rolling forever
    artifact.setLinearDamping(1.5);
    artifact.setAngularDamping(1.5);

    worldWrapper.addBody(artifact);
  }

  private static void spawnArtifacts(AresPhysicsWorld worldWrapper) {
    // 4 rows of 3 balls (Artifacts) on each side
    double[] rowsXRed = {0.6, 0.9, 1.2, 1.5};
    double[] rowsXBlue = {-0.6, -0.9, -1.2, -1.5};
    double[] colsY = {-0.6, 0.0, 0.6};

    // Spawn Red Side Artifacts
    for (double x : rowsXRed) {
      for (double y : colsY) {
        spawnArtifact(worldWrapper, x, y);
      }
    }

    // Spawn Blue Side Artifacts
    for (double x : rowsXBlue) {
      for (double y : colsY) {
        spawnArtifact(worldWrapper, x, y);
      }
    }

    // Staging the 3 starting preloads per robot.
    // Usually held by the robot, but for physics accuracy, we drop them near typical starting
    // zones.
    // Red Alliance Preloads (assuming Red robot starts near x=1.6, y=-1.5)
    spawnArtifact(worldWrapper, 1.6, -1.4);
    spawnArtifact(worldWrapper, 1.6, -1.5);
    spawnArtifact(worldWrapper, 1.6, -1.6);

    // Blue Alliance Preloads (assuming Blue robot starts near x=-1.6, y=1.5)
    spawnArtifact(worldWrapper, -1.6, 1.4);
    spawnArtifact(worldWrapper, -1.6, 1.5);
    spawnArtifact(worldWrapper, -1.6, 1.6);
  }
}

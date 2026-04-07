package org.areslib.core.simulation;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

/**
 * Procedural generation of the DECODE (2025-2026 FTC Game) field elements.
 * Rebuilds the Dyn4j spatial environment for simulation testing.
 */
public class DecodeFieldSim {

    // 5-inch diameter wiffle-ball "Artifacts". 5 inches = 0.127 meters exactly
    public static final double ARTIFACT_RADIUS_METERS = 0.127 / 2.0;

    /**
     * Initializes the DECODE field constraints, bounds, and game objects into the shared physics world.
     */
    public static void buildField() {
        AresPhysicsWorld worldWrapper = AresPhysicsWorld.getInstance();
        worldWrapper.reset(); // Clear old instances

        createFieldBounds(worldWrapper);
        createObelisk(worldWrapper);
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

    private static void createObelisk(AresPhysicsWorld worldWrapper) {
        // The Obelisk is the center structure. We inject a generic static square barrier in the center.
        // Assuming ~ 0.5m x 0.5m footprint.
        Body obelisk = new Body();
        BodyFixture fixture = obelisk.addFixture(Geometry.createRectangle(0.5, 0.5));
        fixture.setRestitution(0.2); // slight bounce
        obelisk.translate(0, 0); // Center of the field (assuming (0,0) center layout)
        obelisk.setMass(MassType.INFINITE);
        worldWrapper.addBody(obelisk);
    }

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

    /**
     * Helper to spawn individual 5-inch artifacts.
     */
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
        // Base coordinate arrays - update these from the latest official DECODE manual (Team Updates)
        // Currently structured as a sample horizontal array in each alliance wing.
        
        // Red Wing Sample Artifacts
        double[][] redStaging = { {1.0, 0.5}, {1.0, 0.0}, {1.0, -0.5} };
        for (double[] pos : redStaging) {
            spawnArtifact(worldWrapper, pos[0], pos[1]);
        }

        // Blue Wing Sample Artifacts
        double[][] blueStaging = { {-1.0, 0.5}, {-1.0, 0.0}, {-1.0, -0.5} };
        for (double[] pos : blueStaging) {
            spawnArtifact(worldWrapper, pos[0], pos[1]);
        }
    }
}

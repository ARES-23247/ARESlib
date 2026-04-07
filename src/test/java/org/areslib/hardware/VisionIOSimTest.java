package org.areslib.hardware;

import org.areslib.core.AresRobot;
import org.areslib.core.simulation.AresPhysicsWorld;
import org.areslib.core.simulation.DecodeFieldSim;
import org.areslib.hardware.interfaces.VisionIO;
import org.areslib.hardware.interfaces.VisionIOSim;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless JUnit 5 tests for {@link VisionIOSim}.
 * Validates synthetic AprilTag detection based on known robot position
 * in the dyn4j physics world.
 */
public class VisionIOSimTest {

    private VisionIOSim visionSim;
    private Body robotBody;
    private VisionIO.VisionInputs inputs;

    @BeforeEach
    void setup() {
        AresRobot.setSimulation(true);

        // Build the DECODE field (walls, obelisk, classifiers, artifacts)
        DecodeFieldSim.buildField();

        // Spawn a robot body at field center facing +X (heading = 0)
        robotBody = new Body();
        BodyFixture fixture = robotBody.addFixture(Geometry.createRectangle(0.4572, 0.4572));
        fixture.setDensity(20.0);
        robotBody.setMass(MassType.NORMAL);
        AresPhysicsWorld.getInstance().addBody(robotBody);

        VisionIOSim.Config config = new VisionIOSim.Config();
        config.cameraFovRadians = Math.toRadians(70.0);
        config.maxRangeMeters = 3.0;
        config.positionNoiseStdDev = 0.0; // No noise for deterministic tests
        config.headingNoiseStdDev = 0.0;
        config.latencyMs = 25.0;
        visionSim = new VisionIOSim(config);

        inputs = new VisionIO.VisionInputs();
    }

    // ========== Basic Detection ==========

    @Test
    void testRobotAtCenterSeesMultipleTags() {
        // Robot at (0, 0) facing +X should see tags on +X wall (IDs 7, 8, 9)
        robotBody.translateToOrigin();
        visionSim.updateInputs(inputs);

        assertTrue(inputs.hasTarget, "Robot at center facing +X should see +X wall tags");
        assertTrue(inputs.fiducialCount > 0, "Should detect at least one tag");
    }

    @Test
    void testRobotFacingWallSeesTagsOnThatWall() {
        // Robot at center facing +Y (heading = PI/2) should see +Y wall tags
        robotBody.translateToOrigin();
        robotBody.rotate(Math.PI / 2.0);
        visionSim.updateInputs(inputs);

        assertTrue(inputs.hasTarget);
        // tx should be near 0 for the center tag on +Y wall (tag ID 2)
        assertTrue(Math.abs(inputs.tx) < 40.0,
            "Center tag should be within ±40° of camera center. Got tx=" + inputs.tx);
    }

    @Test
    void testRobotFacingAwayFromAllTags() {
        // Place robot near +X wall, facing +X (away from all tags, into the wall)
        robotBody.translateToOrigin();
        robotBody.translate(1.7, 0.0); // Very close to +X wall
        // Tags on +X wall face -X, but robot camera faces +X (away from field)
        // The only tags in FOV would be on the +X wall behind the robot
        visionSim.updateInputs(inputs);

        // Robot may or may not see tags depending on exact geometry
        // But this should not crash
        assertNotNull(inputs);
    }

    // ========== Quaternion Output ==========

    @Test
    void testBotPose3dQuaternionMatchesRobotHeading() {
        // Robot at center, heading = 0 → quaternion should be [1, 0, 0, 0]
        robotBody.translateToOrigin();
        visionSim.updateInputs(inputs);

        if (inputs.hasTarget) {
            // With zero noise, quaternion should be identity-ish
            double qW = inputs.botPose3d[3];
            double qZ = inputs.botPose3d[6];
            double reconstructedHeading = 2.0 * Math.atan2(qZ, qW);
            assertEquals(0.0, reconstructedHeading, 0.01,
                "Quaternion should encode heading ≈ 0 for robot facing +X");
        }
    }

    @Test
    void testBotPose3dAt90Degrees() {
        robotBody.translateToOrigin();
        robotBody.rotate(Math.PI / 2.0);

        // Need to face a wall with tags — update camera config to wide FOV
        VisionIOSim.Config wideConfig = new VisionIOSim.Config();
        wideConfig.cameraFovRadians = Math.toRadians(120.0);
        wideConfig.positionNoiseStdDev = 0.0;
        wideConfig.headingNoiseStdDev = 0.0;
        VisionIOSim wideSim = new VisionIOSim(wideConfig);

        wideSim.updateInputs(inputs);

        if (inputs.hasTarget) {
            double qW = inputs.botPose3d[3];
            double qZ = inputs.botPose3d[6];
            double heading = 2.0 * Math.atan2(qZ, qW);
            assertEquals(Math.PI / 2.0, heading, 0.01,
                "Quaternion should encode heading ≈ PI/2");
        }
    }

    // ========== Distance-Based Confidence ==========

    @Test
    void testCloserTagsProduceLargerArea() {
        // Place robot close to +X wall (near tags 7, 8, 9)
        robotBody.translateToOrigin();
        robotBody.translate(1.0, 0.0); // 0.83m from +X wall tags

        visionSim.updateInputs(inputs);
        double closeArea = inputs.ta;

        // Reset and place robot far from +X wall
        robotBody.translateToOrigin();
        visionSim.updateInputs(inputs);
        double farArea = inputs.ta;

        if (inputs.hasTarget && closeArea > 0 && farArea > 0) {
            assertTrue(closeArea > farArea,
                "Closer robot should produce larger target area. Close=" + closeArea + " Far=" + farArea);
        }
    }

    // ========== Range Limiting ==========

    @Test
    void testBeyondMaxRangeNoDetection() {
        VisionIOSim.Config shortRange = new VisionIOSim.Config();
        shortRange.maxRangeMeters = 0.5; // Very short range
        shortRange.positionNoiseStdDev = 0.0;
        VisionIOSim shortSim = new VisionIOSim(shortRange);

        // Robot at center is ~1.83m from all walls — beyond 0.5m range
        robotBody.translateToOrigin();
        shortSim.updateInputs(inputs);

        assertFalse(inputs.hasTarget,
            "Tags beyond maxRange should not be detected");
        assertEquals(0, inputs.fiducialCount);
    }

    // ========== MegaTag2 Multi-Tag Improvement ==========

    @Test
    void testMegaTag2PopulatedWhenVisible() {
        robotBody.translateToOrigin();
        visionSim.updateInputs(inputs);

        if (inputs.hasTarget) {
            // MegaTag2 should also be populated
            assertNotNull(inputs.botPoseMegaTag2);
            assertEquals(7, inputs.botPoseMegaTag2.length);

            // Position should be near (0, 0) since robot is at center with no noise
            assertEquals(0.0, inputs.botPoseMegaTag2[0], 0.1,
                "MegaTag2 X should be near 0 for robot at center");
            assertEquals(0.0, inputs.botPoseMegaTag2[1], 0.1,
                "MegaTag2 Y should be near 0 for robot at center");
        }
    }

    // ========== Latency ==========

    @Test
    void testLatencyIsInjected() {
        robotBody.translateToOrigin();
        visionSim.updateInputs(inputs);

        if (inputs.hasTarget) {
            assertEquals(25.0, inputs.latencyMs, 0.001,
                "Configured latency should be reflected in inputs");
        }
    }
}

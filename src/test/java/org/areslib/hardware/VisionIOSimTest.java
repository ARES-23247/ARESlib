package org.areslib.hardware;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Headless JUnit 5 tests for {@link VisionIOSim}.
 *
 * <p>Validates synthetic AprilTag detection using the DECODE field layout: only 2 tags (Tag 20:
 * Blue Goal at (-1.5, -1.5), Tag 24: Red Goal at (1.5, 1.5)). The Obelisk is outside the field and
 * not simulated.
 */
public class VisionIOSimTest {

  private Body robotBody;
  private VisionIO.VisionInputs inputs;

  /** Wide-FOV, zero-noise config used for most tests to guarantee tag visibility. */
  private VisionIOSim wideSim;

  @BeforeEach
  void setup() {
    AresRobot.setSimulation(true);

    // Build the DECODE field (walls, obelisk obstacle, classifiers, artifacts)
    DecodeFieldSim.buildField();

    // Spawn a robot body at field center facing +X (heading = 0)
    robotBody = new Body();
    BodyFixture fixture = robotBody.addFixture(Geometry.createRectangle(0.4572, 0.4572));
    fixture.setDensity(20.0);
    robotBody.setMass(MassType.NORMAL);
    AresPhysicsWorld.getInstance().addBody(robotBody);

    // Wide FOV so we can reliably see goal tags from most positions.
    // Goals are in far corners — default 70° FOV won't see them from center.
    VisionIOSim.Config wideConfig = new VisionIOSim.Config();
    wideConfig.cameraFovRadians = Math.toRadians(180.0);
    wideConfig.maxRangeMeters = 4.0;
    wideConfig.positionNoiseStdDev = 0.0;
    wideConfig.headingNoiseStdDev = 0.0;
    wideConfig.latencyMs = 25.0;
    wideSim = new VisionIOSim(wideConfig);

    inputs = new VisionIO.VisionInputs();
  }

  // ========== Basic Detection ==========

  @Test
  void testRobotAtCenterSeesGoalTags() {
    // Robot at (0, 0) with 180° FOV should see at least one goal tag
    // Red Goal is at (1.5, 1.5) ~2.12m away, Blue Goal at (-1.5, -1.5) ~2.12m
    robotBody.translateToOrigin();
    wideSim.updateInputs(inputs);

    assertTrue(inputs.hasTarget, "Robot at center with wide FOV should see goal tags");
    assertTrue(inputs.fiducialCount > 0, "Should detect at least one tag");
  }

  @Test
  void testRobotFacingRedGoalSeesIt() {
    // Robot at center facing diagonal toward Red Goal (heading = 45° = PI/4)
    robotBody.translateToOrigin();
    robotBody.rotate(Math.PI / 4.0);

    // Use default narrow FOV (70°) — Red Goal at exactly 45° should be visible
    VisionIOSim.Config narrowConfig = new VisionIOSim.Config();
    narrowConfig.cameraFovRadians = Math.toRadians(70.0);
    narrowConfig.positionNoiseStdDev = 0.0;
    narrowConfig.headingNoiseStdDev = 0.0;
    VisionIOSim narrowSim = new VisionIOSim(narrowConfig);

    narrowSim.updateInputs(inputs);

    assertTrue(inputs.hasTarget, "Robot facing Red Goal should detect it");
    // tx should be near 0 since robot is pointed directly at the tag
    assertTrue(
        Math.abs(inputs.tx) < 15.0,
        "Tag should be near camera center when facing it. Got tx=" + inputs.tx);
  }

  @Test
  void testRobotFacingAwaySeesNothing() {
    // Robot at center facing +X (heading = 0) with narrow 70° FOV
    // Red Goal at 45° and Blue Goal at -135° are both outside ±35° cone
    robotBody.translateToOrigin();

    VisionIOSim.Config narrowConfig = new VisionIOSim.Config();
    narrowConfig.cameraFovRadians = Math.toRadians(70.0);
    narrowConfig.positionNoiseStdDev = 0.0;
    narrowConfig.headingNoiseStdDev = 0.0;
    VisionIOSim narrowSim = new VisionIOSim(narrowConfig);

    narrowSim.updateInputs(inputs);

    assertFalse(
        inputs.hasTarget,
        "Robot facing +X should not see corner-mounted goals with narrow 70° FOV");
  }

  // ========== Quaternion Output ==========

  @Test
  void testBotPose3dQuaternionMatchesRobotHeading() {
    // Robot at center, heading = 0
    robotBody.translateToOrigin();
    wideSim.updateInputs(inputs);

    if (inputs.hasTarget) {
      double qW = inputs.botPose3d[3];
      double qZ = inputs.botPose3d[6];
      double reconstructedHeading = 2.0 * Math.atan2(qZ, qW);
      assertEquals(
          0.0,
          reconstructedHeading,
          0.01,
          "Quaternion should encode heading ≈ 0 for robot facing +X");
    }
  }

  @Test
  void testBotPose3dAt90Degrees() {
    robotBody.translateToOrigin();
    robotBody.rotate(Math.PI / 2.0);

    wideSim.updateInputs(inputs);

    if (inputs.hasTarget) {
      double qW = inputs.botPose3d[3];
      double qZ = inputs.botPose3d[6];
      double heading = 2.0 * Math.atan2(qZ, qW);
      assertEquals(Math.PI / 2.0, heading, 0.01, "Quaternion should encode heading ≈ PI/2");
    }
  }

  // ========== Distance-Based Confidence ==========

  @Test
  void testAreaDecreasesWithDistance() {
    // Close position: (1.3, 1.3) → ~0.28m from Red Goal at (1.5, 1.5)
    org.dyn4j.geometry.Transform closeTransform = new org.dyn4j.geometry.Transform();
    closeTransform.translate(1.3, 1.3);
    robotBody.setTransform(closeTransform);

    wideSim.updateInputs(inputs);
    double closeArea = inputs.ta;
    assertTrue(inputs.hasTarget, "Should see Red Goal from (1.3, 1.3)");

    // Far position: origin (0, 0) → ~2.12m from either goal
    org.dyn4j.geometry.Transform farTransform = new org.dyn4j.geometry.Transform();
    robotBody.setTransform(farTransform);

    VisionIO.VisionInputs farInputs = new VisionIO.VisionInputs();
    wideSim.updateInputs(farInputs);
    double farArea = farInputs.ta;
    assertTrue(farInputs.hasTarget, "Should see a goal from center with 180° FOV");

    assertTrue(
        closeArea > farArea,
        "Closer position should produce larger area. Close=" + closeArea + " Far=" + farArea);
  }

  // ========== Range Limiting ==========

  @Test
  void testBeyondMaxRangeNoDetection() {
    VisionIOSim.Config shortRange = new VisionIOSim.Config();
    shortRange.maxRangeMeters = 0.3; // 30cm — way too short for any goal
    shortRange.positionNoiseStdDev = 0.0;
    shortRange.cameraFovRadians = Math.toRadians(360.0);
    VisionIOSim shortSim = new VisionIOSim(shortRange);

    // Robot at center — both goals are ~2.12m away, well beyond 0.3m range
    robotBody.translateToOrigin();
    shortSim.updateInputs(inputs);

    assertFalse(inputs.hasTarget, "Goals at ~2.12m should not be detected with 0.3m max range");
    assertEquals(0, inputs.fiducialCount);
  }

  // ========== MegaTag2 ==========

  @Test
  void testMegaTag2PopulatedWhenVisible() {
    robotBody.translateToOrigin();
    wideSim.updateInputs(inputs);

    if (inputs.hasTarget) {
      assertNotNull(inputs.botPoseMegaTag2);
      assertEquals(7, inputs.botPoseMegaTag2.length);

      // Position should be near (0, 0) since robot is at center with no noise
      assertEquals(
          0.0, inputs.botPoseMegaTag2[0], 0.1, "MegaTag2 X should be near 0 for robot at center");
      assertEquals(
          0.0, inputs.botPoseMegaTag2[1], 0.1, "MegaTag2 Y should be near 0 for robot at center");
    }
  }

  // ========== Latency ==========

  @Test
  void testLatencyIsInjected() {
    robotBody.translateToOrigin();
    wideSim.updateInputs(inputs);

    if (inputs.hasTarget) {
      assertEquals(
          25.0, inputs.latencyMs, 0.001, "Configured latency should be reflected in inputs");
    }
  }

  // ========== Stability ==========

  @Test
  void testNoTargetDoesNotCrash() {
    // Use impossibly narrow FOV + short range — should see nothing
    VisionIOSim.Config impossibleConfig = new VisionIOSim.Config();
    impossibleConfig.maxRangeMeters = 0.01;
    impossibleConfig.cameraFovRadians = Math.toRadians(1.0);
    VisionIOSim impossibleSim = new VisionIOSim(impossibleConfig);

    robotBody.translateToOrigin();
    assertDoesNotThrow(
        () -> impossibleSim.updateInputs(inputs), "Should not crash when no tags are visible");
    assertFalse(inputs.hasTarget);
  }
}

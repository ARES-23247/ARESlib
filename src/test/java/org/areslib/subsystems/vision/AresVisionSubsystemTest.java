package org.areslib.subsystems.vision;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.hardware.interfaces.VisionIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AresVisionSubsystemTest {

  private AresVisionSubsystem vision;
  private MockVisionIO mockIO;

  private static class MockVisionIO implements VisionIO {
    public final VisionInputs inputs = new VisionInputs();

    @Override
    public void updateInputs(VisionInputs inputs) {
      inputs.hasTarget = this.inputs.hasTarget;
      inputs.tx = this.inputs.tx;
      inputs.ty = this.inputs.ty;
      inputs.ta = this.inputs.ta;
      inputs.latencyMs = this.inputs.latencyMs;
      inputs.pipelineIndex = this.inputs.pipelineIndex;
      inputs.fiducialCount = this.inputs.fiducialCount;
      inputs.minTagAmbiguity = this.inputs.minTagAmbiguity;
      inputs.avgTagDistanceMeters = this.inputs.avgTagDistanceMeters;
      inputs.isMegatag2 = this.inputs.isMegatag2;
      System.arraycopy(this.inputs.botPose3d, 0, inputs.botPose3d, 0, 7);
      System.arraycopy(this.inputs.botPoseMegaTag2, 0, inputs.botPoseMegaTag2, 0, 7);
    }
  }

  @BeforeEach
  void setUp() {
    mockIO = new MockVisionIO();
    // Configure max trust 10.0% (min area logic now defaults to EPSILON)
    vision = new AresVisionSubsystem(mockIO, 10.0);
  }

  private double getExpectedDistanceWeight(double distance) {
    double closeDistance = 2.0;
    double farDistance = 6.0;
    double minWeight = 0.1;
    double baseWeight =
        minWeight
            + (1.0 - minWeight)
                * (1.0
                    / (1.0
                        + Math.exp(
                            (distance - closeDistance) / (farDistance - closeDistance) * 4.0)));

    if (distance > farDistance) {
      double farDistancePenalty = Math.pow(0.5, (distance - farDistance) / 2.0);
      return baseWeight * farDistancePenalty;
    }

    return baseWeight;
  }

  @Test
  void testValidPoseEstimation() {
    // Valid target on the field
    mockIO.inputs.hasTarget = true;
    mockIO.inputs.ta = 5.0; // 50% confidence
    // x, y, z, w, qX, qY, qZ. Let's say robot is at (1.0, 1.0) and perfectly flat
    mockIO.inputs.botPose3d = new double[] {1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0};

    // Update periodic
    vision.periodic();

    assertNotNull(vision.getEstimatedGlobalPose(), "Should return a valid pose");
    assertEquals(1.0, vision.getEstimatedGlobalPose().getX(), 1e-6);
    assertEquals(1.0, vision.getEstimatedGlobalPose().getY(), 1e-6);
  }

  @Test
  void testElevationGhostRejection() {
    mockIO.inputs.hasTarget = true;
    mockIO.inputs.ta = 5.0;
    // Z is way too high (1.0m)
    mockIO.inputs.botPose3d = new double[] {1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0};

    vision.periodic();

    assertNull(
        vision.getEstimatedGlobalPose(), "Pose should be rejected due to impossible Z elevation");
  }

  @Test
  void testOutOfBoundsGhostRejection() {
    mockIO.inputs.hasTarget = true;
    mockIO.inputs.ta = 5.0;
    // X is way off the field (3.0m when MAX is 2.5m)
    mockIO.inputs.botPose3d = new double[] {3.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0};

    vision.periodic();

    assertNull(
        vision.getEstimatedGlobalPose(),
        "Pose should be rejected due to out of bounds X coordinate");
  }

  // Elite B.R.E.A.D Algorithm Tests
  @Test
  void testHighAngularVelocityRejection() {
    mockIO.inputs.hasTarget = true;
    vision.periodic();

    double[] stds = vision.getVisionMeasurementStdDevs(2.0); // 2.0 rad/s > 1.5 threshold
    assertArrayEquals(
        new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
        stds);
  }

  @Test
  void testMultiTagHighlyTrusted() {
    mockIO.inputs.hasTarget = true;
    mockIO.inputs.fiducialCount = 2; // >1 tag
    mockIO.inputs.avgTagDistanceMeters = 2.5; // Simulate distance
    vision.periodic();

    double[] stds = vision.getVisionMeasurementStdDevs(0.0);
    double weight = getExpectedDistanceWeight(2.5);
    assertArrayEquals(new double[] {0.2 / weight, 0.2 / weight, 0.1 / weight}, stds, 1e-4);
  }

  @Test
  void testSingleTagDisambiguationRejection() {
    mockIO.inputs.hasTarget = true;
    mockIO.inputs.fiducialCount = 1;
    mockIO.inputs.minTagAmbiguity = 0.2; // 0.2 > 0.15 limit
    vision.periodic();

    double[] stds = vision.getVisionMeasurementStdDevs(0.0);
    assertNull(stds, "High ambiguity should cause vision to be rejected.");
  }

  @Test
  void testSingleTagPolynomialDistanceScale() {
    mockIO.inputs.hasTarget = true;
    mockIO.inputs.fiducialCount = 1;
    mockIO.inputs.minTagAmbiguity = 0.1;
    mockIO.inputs.avgTagDistanceMeters = 2.5; // test 2.5 meters away (under 3.0 limit)
    vision.periodic();

    double[] stds = vision.getVisionMeasurementStdDevs(0.0);
    double weight = getExpectedDistanceWeight(2.5);
    // B.R.E.A.D Equation is 0.03*d^2, 0.05*d^2 modified by weight
    assertEquals((0.03 * 6.25) / weight, stds[0], 1e-4);
    assertEquals((0.05 * 6.25) / weight, stds[2], 1e-4);
  }

  @Test
  void testSingleTagDistanceCutoff2025() {
    mockIO.inputs.hasTarget = true;
    mockIO.inputs.fiducialCount = 1;
    mockIO.inputs.minTagAmbiguity = 0.1;
    mockIO.inputs.avgTagDistanceMeters = 3.5; // Elite 2025 Cutoff is 3.0 meters
    vision.periodic();

    double[] stds = vision.getVisionMeasurementStdDevs(0.0);
    assertNull(stds, "Single tags further than 3.0m should be rejected per B.R.E.A.D. 2025 rules.");
  }

  @Test
  void testSingleTagDistanceFallback() {
    mockIO.inputs.hasTarget = true;
    mockIO.inputs.fiducialCount = 1;
    mockIO.inputs.minTagAmbiguity = 0.1;
    mockIO.inputs.avgTagDistanceMeters = 0; // Simulate Network table drop
    mockIO.inputs.ta = 5.0; // Area = 5%
    vision.periodic();

    double[] stds = vision.getVisionMeasurementStdDevs(0.0);
    // Distance fallback: 3.0 / sqrt(5.0) = 1.34164
    double dist = 3.0 / Math.sqrt(5.0);
    double weight = getExpectedDistanceWeight(dist);
    assertEquals((0.03 * dist * dist) / weight, stds[0], 1e-4);
  }
}

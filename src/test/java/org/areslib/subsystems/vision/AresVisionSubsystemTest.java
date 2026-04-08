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
      System.arraycopy(this.inputs.botPose3d, 0, inputs.botPose3d, 0, 7);
      System.arraycopy(this.inputs.botPoseMegaTag2, 0, inputs.botPoseMegaTag2, 0, 7);
    }
  }

  @BeforeEach
  void setUp() {
    mockIO = new MockVisionIO();
    // Configure min area 1.0%, max trust 10.0%
    vision = new AresVisionSubsystem(mockIO, 1.0, 10.0);
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
    assertEquals(0.5, vision.getPoseConfidence(), 1e-6);
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
}

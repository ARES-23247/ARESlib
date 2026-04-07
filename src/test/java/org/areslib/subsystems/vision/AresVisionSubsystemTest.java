package org.areslib.subsystems.vision;

import org.areslib.hardware.interfaces.VisionIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless JUnit 5 tests for {@link AresVisionSubsystem}.
 * Validates quaternion→yaw extraction, ghost rejection, field bounds checking,
 * and confidence scoring — all without Android dependencies.
 */
public class AresVisionSubsystemTest {

    /** A mock VisionIO that lets us inject arbitrary vision data per-test. */
    static class MockVisionIO implements VisionIO {
        final VisionInputs mockInputs = new VisionInputs();

        @Override
        public void updateInputs(VisionInputs inputs) {
            inputs.hasTarget = mockInputs.hasTarget;
            inputs.tx = mockInputs.tx;
            inputs.ty = mockInputs.ty;
            inputs.ta = mockInputs.ta;
            System.arraycopy(mockInputs.botPose3d, 0, inputs.botPose3d, 0, 7);
            System.arraycopy(mockInputs.botPoseMegaTag2, 0, inputs.botPoseMegaTag2, 0, 7);
            inputs.latencyMs = mockInputs.latencyMs;
            inputs.pipelineIndex = mockInputs.pipelineIndex;
            inputs.fiducialCount = mockInputs.fiducialCount;
        }
    }

    private MockVisionIO mockIO;
    private AresVisionSubsystem vision;

    // Using 0.5% min, 5.0% max trust for testing
    private static final double MIN_AREA = 0.5;
    private static final double MAX_TRUST_AREA = 5.0;

    @BeforeEach
    void setup() {
        mockIO = new MockVisionIO();
        vision = new AresVisionSubsystem(mockIO, MIN_AREA, MAX_TRUST_AREA);
    }

    // ========== Basic Target Detection ==========

    @Test
    void testNoTargetReturnsNull() {
        mockIO.mockInputs.hasTarget = false;
        vision.periodic();

        assertFalse(vision.hasTarget());
        assertNull(vision.getEstimatedGlobalPose());
        assertEquals(0.0, vision.getPoseConfidence());
    }

    @Test
    void testHasTargetPassthrough() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.tx = 5.3;
        mockIO.mockInputs.ty = -2.1;
        mockIO.mockInputs.ta = 3.0;
        vision.periodic();

        assertTrue(vision.hasTarget());
        assertEquals(5.3, vision.getTargetXOffset(), 1e-9);
        assertEquals(-2.1, vision.getTargetYOffset(), 1e-9);
        assertEquals(3.0, vision.getTargetArea(), 1e-9);
    }

    // ========== Quaternion → Yaw Extraction ==========

    @Test
    void testQuaternionToYawIdentity() {
        // Quaternion [1,0,0,0] = no rotation → yaw should be 0
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        mockIO.mockInputs.botPose3d = new double[]{0.5, 0.5, 0.0, 1.0, 0.0, 0.0, 0.0};
        vision.periodic();

        com.pedropathing.geometry.Pose pose = vision.getEstimatedGlobalPose();
        assertNotNull(pose);
        assertEquals(0.0, pose.getHeading(), 1e-6, "Identity quaternion should yield 0 yaw");
    }

    @Test
    void testQuaternionToYaw90Degrees() {
        // Quaternion for 90° yaw: w=cos(45°)=0.7071, qZ=sin(45°)=0.7071, qX=qY=0
        double angle = Math.PI / 2.0;
        double w = Math.cos(angle / 2.0);
        double qZ = Math.sin(angle / 2.0);

        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        mockIO.mockInputs.botPose3d = new double[]{0.5, 0.5, 0.0, w, 0.0, 0.0, qZ};
        vision.periodic();

        com.pedropathing.geometry.Pose pose = vision.getEstimatedGlobalPose();
        assertNotNull(pose);
        assertEquals(Math.PI / 2.0, pose.getHeading(), 1e-4,
            "Quaternion with 90° Z rotation should yield PI/2 yaw");
    }

    @Test
    void testQuaternionToYawNegative45() {
        double angle = -Math.PI / 4.0;
        double w = Math.cos(angle / 2.0);
        double qZ = Math.sin(angle / 2.0);

        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        mockIO.mockInputs.botPose3d = new double[]{0.5, 0.5, 0.0, w, 0.0, 0.0, qZ};
        vision.periodic();

        com.pedropathing.geometry.Pose pose = vision.getEstimatedGlobalPose();
        assertNotNull(pose);
        assertEquals(-Math.PI / 4.0, pose.getHeading(), 1e-4);
    }

    // ========== Ghost Rejection (Z-Elevation) ==========

    @Test
    void testGhostRejectionRobotFloating() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        // Z = 2.0 meters — robot is "floating" above field (MAX_ELEVATION_METERS = 0.5)
        mockIO.mockInputs.botPose3d = new double[]{0.5, 0.5, 2.0, 1.0, 0.0, 0.0, 0.0};
        vision.periodic();

        assertNull(vision.getEstimatedGlobalPose(),
            "Pose with Z > MAX_ELEVATION_METERS should be rejected as ghost reflection");
    }

    @Test
    void testGhostRejectionRobotUnderground() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        // Z = -1.0 meters — robot is "underground"
        mockIO.mockInputs.botPose3d = new double[]{0.5, 0.5, -1.0, 1.0, 0.0, 0.0, 0.0};
        vision.periodic();

        assertNull(vision.getEstimatedGlobalPose(),
            "Pose with Z < -MAX_ELEVATION_METERS should be rejected");
    }

    @Test
    void testGhostAcceptedWithinElevationTolerance() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        // Z = 0.1 meters — normal robot on the ground with slight measurement noise
        mockIO.mockInputs.botPose3d = new double[]{0.5, 0.5, 0.1, 1.0, 0.0, 0.0, 0.0};
        vision.periodic();

        assertNotNull(vision.getEstimatedGlobalPose(),
            "Pose within elevation tolerance should be accepted");
    }

    // ========== Field Bounds Checking ==========

    @Test
    void testFieldBoundsRejectionTooFarPositive() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        // X = 5.0 meters — way outside the field (MAX_VISION_POSITION_METERS = 2.5)
        mockIO.mockInputs.botPose3d = new double[]{5.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0};
        vision.periodic();

        assertNull(vision.getEstimatedGlobalPose(),
            "Pose with |X| > MAX_VISION_POSITION_METERS should be rejected");
    }

    @Test
    void testFieldBoundsRejectionTooFarNegative() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        // Y = -3.5 meters
        mockIO.mockInputs.botPose3d = new double[]{0.0, -3.5, 0.0, 1.0, 0.0, 0.0, 0.0};
        vision.periodic();

        assertNull(vision.getEstimatedGlobalPose());
    }

    @Test
    void testFieldBoundsAcceptedWithinAllowedRange() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 3.0;
        // (1.5, -1.0) is within the 2.5m boundary
        mockIO.mockInputs.botPose3d = new double[]{1.5, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0};
        vision.periodic();

        assertNotNull(vision.getEstimatedGlobalPose());
    }

    // ========== Confidence Scoring ==========

    @Test
    void testConfidenceZeroWhenNoTarget() {
        mockIO.mockInputs.hasTarget = false;
        vision.periodic();
        assertEquals(0.0, vision.getPoseConfidence());
    }

    @Test
    void testConfidenceZeroWhenBelowMinArea() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 0.1; // Below MIN_AREA (0.5)
        vision.periodic();
        assertEquals(0.0, vision.getPoseConfidence());
    }

    @Test
    void testConfidenceLinearScaling() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 2.5; // Half of MAX_TRUST_AREA (5.0)
        vision.periodic();
        assertEquals(0.5, vision.getPoseConfidence(), 1e-6,
            "Confidence should be ta/maxTrustArea = 2.5/5.0 = 0.5");
    }

    @Test
    void testConfidenceCappedAtOne() {
        mockIO.mockInputs.hasTarget = true;
        mockIO.mockInputs.ta = 10.0; // Way above MAX_TRUST_AREA
        vision.periodic();
        assertEquals(1.0, vision.getPoseConfidence(), 1e-6,
            "Confidence should be capped at 1.0");
    }

    // ========== Pipeline Switching ==========

    @Test
    void testSetPipelineDelegatesToIO() {
        // Just verifying no exception is thrown
        vision.setPipeline(2);
    }
}

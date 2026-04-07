package org.areslib.math.kinematics;

import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MecanumDriveKinematics}.
 */
class MecanumDriveKinematicsTest {

    private static final double EPSILON = 1e-4;

    // Standard symmetric mecanum: modules at ±0.3m
    private final MecanumDriveKinematics kinematics = new MecanumDriveKinematics(
        new Translation2d(0.3, 0.3),   // front left
        new Translation2d(0.3, -0.3),  // front right
        new Translation2d(-0.3, 0.3),  // rear left
        new Translation2d(-0.3, -0.3)  // rear right
    );

    @Test
    @DisplayName("Standard symmetric config constructs without singularity")
    void symmetricConfigConstructs() {
        // Should NOT throw after the fix
        assertNotNull(kinematics);
    }

    @Test
    @DisplayName("Zero chassis speeds produces zero wheel speeds")
    void zeroSpeedsZeroWheels() {
        MecanumDriveWheelSpeeds ws = kinematics.toWheelSpeeds(new ChassisSpeeds(0, 0, 0));
        assertEquals(0.0, ws.frontLeftMetersPerSecond, EPSILON);
        assertEquals(0.0, ws.frontRightMetersPerSecond, EPSILON);
        assertEquals(0.0, ws.rearLeftMetersPerSecond, EPSILON);
        assertEquals(0.0, ws.rearRightMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Pure forward: all wheels move forward")
    void pureForward() {
        MecanumDriveWheelSpeeds ws = kinematics.toWheelSpeeds(new ChassisSpeeds(2.0, 0, 0));
        assertEquals(2.0, ws.frontLeftMetersPerSecond, EPSILON);
        assertEquals(2.0, ws.frontRightMetersPerSecond, EPSILON);
        assertEquals(2.0, ws.rearLeftMetersPerSecond, EPSILON);
        assertEquals(2.0, ws.rearRightMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Pure strafe right: correct mecanum roller pattern")
    void pureStrafeRight() {
        MecanumDriveWheelSpeeds ws = kinematics.toWheelSpeeds(new ChassisSpeeds(0, 1.0, 0));
        // Mecanum strafe: FL and RR spin opposite to FR and RL
        // With col1 pattern [-1, +1, -1, +1]: FL=-1, FR=+1, RL=-1, RR=+1
        assertEquals(-1.0, ws.frontLeftMetersPerSecond, EPSILON);
        assertEquals(1.0, ws.frontRightMetersPerSecond, EPSILON);
        assertEquals(-1.0, ws.rearLeftMetersPerSecond, EPSILON);
        assertEquals(1.0, ws.rearRightMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Forward/inverse round-trip (pure forward)")
    void roundTripForward() {
        ChassisSpeeds original = new ChassisSpeeds(2.0, 0, 0);
        MecanumDriveWheelSpeeds ws = kinematics.toWheelSpeeds(original);
        ChassisSpeeds recovered = kinematics.toChassisSpeeds(ws);
        assertEquals(original.vxMetersPerSecond, recovered.vxMetersPerSecond, EPSILON);
        assertEquals(original.vyMetersPerSecond, recovered.vyMetersPerSecond, EPSILON);
        assertEquals(original.omegaRadiansPerSecond, recovered.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Forward/inverse round-trip (pure strafe)")
    void roundTripStrafe() {
        ChassisSpeeds original = new ChassisSpeeds(0, 2.0, 0);
        MecanumDriveWheelSpeeds ws = kinematics.toWheelSpeeds(original);
        ChassisSpeeds recovered = kinematics.toChassisSpeeds(ws);
        assertEquals(original.vxMetersPerSecond, recovered.vxMetersPerSecond, EPSILON);
        assertEquals(original.vyMetersPerSecond, recovered.vyMetersPerSecond, EPSILON);
        assertEquals(original.omegaRadiansPerSecond, recovered.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Forward/inverse round-trip (pure rotation)")
    void roundTripRotation() {
        ChassisSpeeds original = new ChassisSpeeds(0, 0, 1.0);
        MecanumDriveWheelSpeeds ws = kinematics.toWheelSpeeds(original);
        ChassisSpeeds recovered = kinematics.toChassisSpeeds(ws);
        assertEquals(original.vxMetersPerSecond, recovered.vxMetersPerSecond, EPSILON);
        assertEquals(original.vyMetersPerSecond, recovered.vyMetersPerSecond, EPSILON);
        assertEquals(original.omegaRadiansPerSecond, recovered.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Forward/inverse round-trip (combined motion)")
    void roundTripCombined() {
        ChassisSpeeds original = new ChassisSpeeds(1.5, 0.8, 0.3);
        MecanumDriveWheelSpeeds ws = kinematics.toWheelSpeeds(original);
        ChassisSpeeds recovered = kinematics.toChassisSpeeds(ws);
        assertEquals(original.vxMetersPerSecond, recovered.vxMetersPerSecond, EPSILON);
        assertEquals(original.vyMetersPerSecond, recovered.vyMetersPerSecond, EPSILON);
        assertEquals(original.omegaRadiansPerSecond, recovered.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("toTwist2d with equal wheel deltas produces pure forward")
    void toTwist2dForward() {
        MecanumDriveWheelPositions start = new MecanumDriveWheelPositions(0, 0, 0, 0);
        MecanumDriveWheelPositions end = new MecanumDriveWheelPositions(1, 1, 1, 1);
        
        org.areslib.math.geometry.Twist2d twist = kinematics.toTwist2d(start, end);
        assertTrue(twist.dx > 0, "Should have forward motion");
        assertEquals(0.0, twist.dy, EPSILON);
        assertEquals(0.0, twist.dtheta, EPSILON);
    }

    @Test
    @DisplayName("Combined motion sums linearly")
    void linearSuperposition() {
        MecanumDriveWheelSpeeds ws1 = kinematics.toWheelSpeeds(new ChassisSpeeds(1, 0, 0));
        MecanumDriveWheelSpeeds ws2 = kinematics.toWheelSpeeds(new ChassisSpeeds(0, 1, 0));
        MecanumDriveWheelSpeeds combined = kinematics.toWheelSpeeds(new ChassisSpeeds(1, 1, 0));
        
        assertEquals(ws1.frontLeftMetersPerSecond + ws2.frontLeftMetersPerSecond,
                     combined.frontLeftMetersPerSecond, EPSILON);
        assertEquals(ws1.frontRightMetersPerSecond + ws2.frontRightMetersPerSecond,
                     combined.frontRightMetersPerSecond, EPSILON);
        assertEquals(ws1.rearLeftMetersPerSecond + ws2.rearLeftMetersPerSecond,
                     combined.rearLeftMetersPerSecond, EPSILON);
        assertEquals(ws1.rearRightMetersPerSecond + ws2.rearRightMetersPerSecond,
                     combined.rearRightMetersPerSecond, EPSILON);
    }
}

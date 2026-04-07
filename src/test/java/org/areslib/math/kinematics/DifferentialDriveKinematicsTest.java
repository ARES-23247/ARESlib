package org.areslib.math.kinematics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DifferentialDriveKinematics}.
 */
class DifferentialDriveKinematicsTest {

    private static final double EPSILON = 1e-6;
    private final DifferentialDriveKinematics kinematics = new DifferentialDriveKinematics(0.6);

    @Test
    @DisplayName("Zero chassis speeds produces zero wheel speeds")
    void zeroSpeeds() {
        DifferentialDriveWheelSpeeds ws = kinematics.toWheelSpeeds(new ChassisSpeeds());
        assertEquals(0.0, ws.leftMetersPerSecond, EPSILON);
        assertEquals(0.0, ws.rightMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Pure forward produces equal wheel speeds")
    void pureForward() {
        DifferentialDriveWheelSpeeds ws = kinematics.toWheelSpeeds(new ChassisSpeeds(2.0, 0, 0));
        assertEquals(2.0, ws.leftMetersPerSecond, EPSILON);
        assertEquals(2.0, ws.rightMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Pure rotation produces opposite wheel speeds")
    void pureRotation() {
        DifferentialDriveWheelSpeeds ws = kinematics.toWheelSpeeds(new ChassisSpeeds(0, 0, 1.0));
        // left = 0 - 0.3 * 1 = -0.3, right = 0 + 0.3 * 1 = 0.3
        assertEquals(-0.3, ws.leftMetersPerSecond, EPSILON);
        assertEquals(0.3, ws.rightMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("Forward/inverse round-trip")
    void roundTrip() {
        ChassisSpeeds original = new ChassisSpeeds(2.0, 0, 0.5);
        DifferentialDriveWheelSpeeds ws = kinematics.toWheelSpeeds(original);
        ChassisSpeeds recovered = kinematics.toChassisSpeeds(ws);
        assertEquals(original.vxMetersPerSecond, recovered.vxMetersPerSecond, EPSILON);
        assertEquals(0.0, recovered.vyMetersPerSecond, EPSILON); // diff drive can't strafe
        assertEquals(original.omegaRadiansPerSecond, recovered.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("toTwist2d computes correct twist from wheel deltas")
    void toTwist2d() {
        DifferentialDriveWheelPositions start = new DifferentialDriveWheelPositions(0, 0);
        DifferentialDriveWheelPositions end = new DifferentialDriveWheelPositions(1, 1);
        
        org.areslib.math.geometry.Twist2d twist = kinematics.toTwist2d(start, end);
        assertEquals(1.0, twist.dx, EPSILON); // pure forward
        assertEquals(0.0, twist.dy, EPSILON);
        assertEquals(0.0, twist.dtheta, EPSILON);
    }

    @Test
    @DisplayName("toTwist2d with rotation")
    void toTwist2dRotation() {
        DifferentialDriveWheelPositions start = new DifferentialDriveWheelPositions(0, 0);
        DifferentialDriveWheelPositions end = new DifferentialDriveWheelPositions(-0.3, 0.3);
        
        org.areslib.math.geometry.Twist2d twist = kinematics.toTwist2d(start, end);
        assertEquals(0.0, twist.dx, EPSILON); // pure rotation
        assertEquals(1.0, twist.dtheta, EPSILON); // (0.3 - (-0.3)) / 0.6 = 1.0
    }
}

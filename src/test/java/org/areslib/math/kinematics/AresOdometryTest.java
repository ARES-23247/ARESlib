package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AresOdometry}.
 */
class AresOdometryTest {

    private static final double EPSILON = 1e-4;

    @Test
    @DisplayName("Default constructor starts at origin")
    void defaultOrigin() {
        AresOdometry odom = new AresOdometry();
        assertEquals(0.0, odom.getPose().getX(), EPSILON);
        assertEquals(0.0, odom.getPose().getY(), EPSILON);
        assertEquals(0.0, odom.getPose().getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("Custom initial pose is preserved")
    void customInitialPose() {
        Pose2d initial = new Pose2d(3, 4, new Rotation2d(1.0));
        AresOdometry odom = new AresOdometry(initial);
        assertEquals(3.0, odom.getPose().getX(), EPSILON);
        assertEquals(4.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Pure forward at 0° heading moves along +X")
    void pureForwardX() {
        AresOdometry odom = new AresOdometry();
        odom.update(new Rotation2d(0), new ChassisSpeeds(2.0, 0, 0), 0.5);
        // dx = 2.0 * 0.5 = 1.0, heading 0° so field X += 1.0
        assertEquals(1.0, odom.getPose().getX(), EPSILON);
        assertEquals(0.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Pure strafe at 0° heading moves along +Y")
    void pureStrafeY() {
        AresOdometry odom = new AresOdometry();
        odom.update(new Rotation2d(0), new ChassisSpeeds(0, 2.0, 0), 0.5);
        assertEquals(0.0, odom.getPose().getX(), EPSILON);
        assertEquals(1.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Forward at 90° heading moves along +Y")
    void forwardAt90() {
        AresOdometry odom = new AresOdometry(new Pose2d(0, 0, new Rotation2d(Math.PI / 2)));
        odom.update(new Rotation2d(Math.PI / 2), new ChassisSpeeds(2.0, 0, 0), 0.5);
        // Forward motion rotated by 90° → moves in +Y
        assertEquals(0.0, odom.getPose().getX(), EPSILON);
        assertEquals(1.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Cumulative updates accumulate position")
    void cumulativeUpdates() {
        AresOdometry odom = new AresOdometry();
        odom.update(new Rotation2d(0), new ChassisSpeeds(1.0, 0, 0), 0.5); // x = 0.5
        odom.update(new Rotation2d(0), new ChassisSpeeds(1.0, 0, 0), 0.5); // x = 1.0
        assertEquals(1.0, odom.getPose().getX(), EPSILON);
    }

    @Test
    @DisplayName("Gyro angle is used as absolute heading")
    void gyroAbsoluteHeading() {
        AresOdometry odom = new AresOdometry();
        odom.update(new Rotation2d(1.0), new ChassisSpeeds(0, 0, 0), 0.02);
        assertEquals(1.0, odom.getPose().getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("Arc integration for turning motion")
    void arcIntegration() {
        AresOdometry odom = new AresOdometry();
        // Drive forward while turning 90°
        odom.update(new Rotation2d(Math.PI / 2), new ChassisSpeeds(2.0, 0, 0), 1.0);
        // Arc motion should place robot to the right and forward
        assertTrue(odom.getPose().getX() > 0, "Should have +X component from arc");
        assertTrue(odom.getPose().getY() > 0, "Should have +Y component from arc");
    }

    @Test
    @DisplayName("Zero dt produces no movement")
    void zeroDt() {
        AresOdometry odom = new AresOdometry();
        odom.update(new Rotation2d(0), new ChassisSpeeds(100, 100, 0), 0.0);
        assertEquals(0.0, odom.getPose().getX(), EPSILON);
        assertEquals(0.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("resetPosition sets new pose")
    void resetPosition() {
        AresOdometry odom = new AresOdometry();
        odom.update(new Rotation2d(0), new ChassisSpeeds(5, 0, 0), 1.0);
        
        Pose2d newPose = new Pose2d(100, 200, new Rotation2d(2.0));
        odom.resetPosition(newPose);
        assertEquals(100.0, odom.getPose().getX(), EPSILON);
        assertEquals(200.0, odom.getPose().getY(), EPSILON);
        assertEquals(2.0, odom.getPose().getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("Straight line path uses direct integration (no arc)")
    void straightLineNoArc() {
        AresOdometry odom = new AresOdometry();
        // No rotation → straight line path
        odom.update(new Rotation2d(0), new ChassisSpeeds(3.0, 1.0, 0), 1.0);
        assertEquals(3.0, odom.getPose().getX(), EPSILON);
        assertEquals(1.0, odom.getPose().getY(), EPSILON);
    }
}

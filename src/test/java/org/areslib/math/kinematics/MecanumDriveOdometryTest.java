package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MecanumDriveOdometry}.
 */
class MecanumDriveOdometryTest {

    private static final double EPSILON = 1e-4;

    private final MecanumDriveKinematics kinematics = new MecanumDriveKinematics(
        new Translation2d(0.3, 0.3),
        new Translation2d(0.3, -0.3),
        new Translation2d(-0.3, 0.3),
        new Translation2d(-0.3, -0.3)
    );

    @Test
    @DisplayName("Initial pose is preserved")
    void initialPose() {
        Pose2d initial = new Pose2d(5, 10, new Rotation2d(0.3));
        MecanumDriveOdometry odom = new MecanumDriveOdometry(
            kinematics, initial.getRotation(),
            new MecanumDriveWheelPositions(0, 0, 0, 0), initial);
        assertEquals(5.0, odom.getPose().getX(), EPSILON);
        assertEquals(10.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Default initial pose is origin")
    void defaultPose() {
        MecanumDriveOdometry odom = new MecanumDriveOdometry(
            kinematics, new Rotation2d(), new MecanumDriveWheelPositions(0, 0, 0, 0));
        assertEquals(0.0, odom.getPose().getX(), EPSILON);
        assertEquals(0.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Pure forward motion: all wheels advance equally")
    void pureForward() {
        MecanumDriveOdometry odom = new MecanumDriveOdometry(
            kinematics, new Rotation2d(), new MecanumDriveWheelPositions(0, 0, 0, 0));
        odom.update(new Rotation2d(0), new MecanumDriveWheelPositions(1, 1, 1, 1));
        assertTrue(odom.getPose().getX() > 0.5, "Should move forward");
        assertEquals(0.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("resetPosition resets everything")
    void resetPosition() {
        MecanumDriveOdometry odom = new MecanumDriveOdometry(
            kinematics, new Rotation2d(), new MecanumDriveWheelPositions(0, 0, 0, 0));
        odom.update(new Rotation2d(0), new MecanumDriveWheelPositions(5, 5, 5, 5));
        
        Pose2d newPose = new Pose2d(10, 20, new Rotation2d(1.0));
        odom.resetPosition(new Rotation2d(1.0), new MecanumDriveWheelPositions(0, 0, 0, 0), newPose);
        assertEquals(10.0, odom.getPose().getX(), EPSILON);
        assertEquals(20.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("resetTranslation only changes pose")
    void resetTranslation() {
        MecanumDriveOdometry odom = new MecanumDriveOdometry(
            kinematics, new Rotation2d(), new MecanumDriveWheelPositions(0, 0, 0, 0));
        odom.resetTranslation(new Pose2d(99, 99, new Rotation2d()));
        assertEquals(99.0, odom.getPose().getX(), EPSILON);
    }

    @Test
    @DisplayName("Gyro overrides wheel-derived heading")
    void gyroOverrides() {
        MecanumDriveOdometry odom = new MecanumDriveOdometry(
            kinematics, new Rotation2d(), new MecanumDriveWheelPositions(0, 0, 0, 0));
        odom.update(new Rotation2d(0.5), new MecanumDriveWheelPositions(1, 1, 1, 1));
        assertEquals(0.5, odom.getPose().getRotation().getRadians(), EPSILON);
    }
}

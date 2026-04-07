package org.areslib.math.kinematics;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SwerveDriveOdometry}.
 */
class SwerveDriveOdometryTest {

    private static final double EPSILON = 1e-4;

    private final SwerveDriveKinematics kinematics = new SwerveDriveKinematics(
        new Translation2d(0.3, 0.3),
        new Translation2d(0.3, -0.3),
        new Translation2d(-0.3, 0.3),
        new Translation2d(-0.3, -0.3)
    );

    private SwerveModulePosition[] makePositions(double fl, double fr, double rl, double rr) {
        return new SwerveModulePosition[] {
            new SwerveModulePosition(fl, new Rotation2d(0)),
            new SwerveModulePosition(fr, new Rotation2d(0)),
            new SwerveModulePosition(rl, new Rotation2d(0)),
            new SwerveModulePosition(rr, new Rotation2d(0))
        };
    }

    @Test
    @DisplayName("Initial pose is preserved")
    void initialPose() {
        Pose2d initial = new Pose2d(3, 4, new Rotation2d(1.0));
        SwerveDriveOdometry odom = new SwerveDriveOdometry(
            kinematics, initial.getRotation(), makePositions(0, 0, 0, 0), initial);
        assertEquals(3.0, odom.getPose().getX(), EPSILON);
        assertEquals(4.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Default initial pose is origin")
    void defaultPose() {
        SwerveDriveOdometry odom = new SwerveDriveOdometry(
            kinematics, new Rotation2d(), makePositions(0, 0, 0, 0));
        assertEquals(0.0, odom.getPose().getX(), EPSILON);
        assertEquals(0.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Pure forward motion along X axis")
    void pureForward() {
        SwerveDriveOdometry odom = new SwerveDriveOdometry(
            kinematics, new Rotation2d(), makePositions(0, 0, 0, 0));
        odom.update(new Rotation2d(0), makePositions(1, 1, 1, 1));
        assertEquals(1.0, odom.getPose().getX(), EPSILON);
        assertEquals(0.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("Multiple updates accumulate correctly")
    void cumulativeUpdates() {
        SwerveDriveOdometry odom = new SwerveDriveOdometry(
            kinematics, new Rotation2d(), makePositions(0, 0, 0, 0));
        odom.update(new Rotation2d(0), makePositions(0.5, 0.5, 0.5, 0.5));
        odom.update(new Rotation2d(0), makePositions(1.0, 1.0, 1.0, 1.0));
        assertEquals(1.0, odom.getPose().getX(), EPSILON);
    }

    @Test
    @DisplayName("resetPosition resets pose and wheel state")
    void resetPosition() {
        SwerveDriveOdometry odom = new SwerveDriveOdometry(
            kinematics, new Rotation2d(), makePositions(0, 0, 0, 0));
        odom.update(new Rotation2d(0), makePositions(5, 5, 5, 5));
        
        Pose2d newPose = new Pose2d(10, 20, new Rotation2d(1.0));
        odom.resetPosition(new Rotation2d(1.0), makePositions(0, 0, 0, 0), newPose);
        assertEquals(10.0, odom.getPose().getX(), EPSILON);
        assertEquals(20.0, odom.getPose().getY(), EPSILON);
    }

    @Test
    @DisplayName("resetTranslation only changes pose")
    void resetTranslation() {
        SwerveDriveOdometry odom = new SwerveDriveOdometry(
            kinematics, new Rotation2d(), makePositions(0, 0, 0, 0));
        odom.resetTranslation(new Pose2d(99, 99, new Rotation2d()));
        assertEquals(99.0, odom.getPose().getX(), EPSILON);
    }

    @Test
    @DisplayName("update throws on wrong module count")
    void wrongModuleCount() {
        SwerveDriveOdometry odom = new SwerveDriveOdometry(
            kinematics, new Rotation2d(), makePositions(0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
            odom.update(new Rotation2d(0), new SwerveModulePosition[] {
                new SwerveModulePosition(0, new Rotation2d())
            }));
    }

    @Test
    @DisplayName("Gyro angle overrides wheel-derived theta")
    void gyroOverridesTheta() {
        SwerveDriveOdometry odom = new SwerveDriveOdometry(
            kinematics, new Rotation2d(), makePositions(0, 0, 0, 0));
        // All wheels forward but gyro says we rotated
        odom.update(new Rotation2d(0.5), makePositions(1, 1, 1, 1));
        assertEquals(0.5, odom.getPose().getRotation().getRadians(), EPSILON);
    }
}

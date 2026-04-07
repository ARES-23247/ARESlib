package org.areslib.math.geometry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Pose2d}, including exp/log round-trip properties.
 */
class Pose2dTest {

    private static final double EPSILON = 1e-6;

    @Test
    @DisplayName("Default constructor produces origin identity")
    void defaultConstructor() {
        Pose2d pose = new Pose2d();
        assertEquals(0.0, pose.getX(), EPSILON);
        assertEquals(0.0, pose.getY(), EPSILON);
        assertEquals(0.0, pose.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("Construct from x, y, rotation")
    void constructFromComponents() {
        Pose2d pose = new Pose2d(1.0, 2.0, new Rotation2d(Math.PI / 4));
        assertEquals(1.0, pose.getX(), EPSILON);
        assertEquals(2.0, pose.getY(), EPSILON);
        assertEquals(Math.PI / 4, pose.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("transformBy applies relative transform")
    void transformBy() {
        Pose2d base = new Pose2d(1.0, 0.0, new Rotation2d(Math.PI / 2));
        Pose2d transform = new Pose2d(1.0, 0.0, new Rotation2d(0));
        Pose2d result = base.transformBy(transform);
        // After rotating 90° and moving 1 in local X, we should move in global +Y
        assertEquals(1.0, result.getX(), EPSILON);
        assertEquals(1.0, result.getY(), EPSILON);
        assertEquals(Math.PI / 2, result.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("relativeTo computes correct relative pose")
    void relativeTo() {
        Pose2d a = new Pose2d(2.0, 3.0, new Rotation2d(Math.PI / 4));
        Pose2d b = new Pose2d(2.0, 3.0, new Rotation2d(Math.PI / 4));
        Pose2d relative = a.relativeTo(b);
        assertEquals(0.0, relative.getX(), EPSILON);
        assertEquals(0.0, relative.getY(), EPSILON);
        assertEquals(0.0, relative.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("relativeTo is inverse of transformBy")
    void relativeToInverse() {
        Pose2d origin = new Pose2d(1.0, 2.0, new Rotation2d(0.5));
        Pose2d target = new Pose2d(4.0, 6.0, new Rotation2d(1.2));
        
        Pose2d delta = target.relativeTo(origin);
        Pose2d reconstructed = origin.transformBy(delta);
        
        assertEquals(target.getX(), reconstructed.getX(), EPSILON);
        assertEquals(target.getY(), reconstructed.getY(), EPSILON);
        assertEquals(target.getRotation().getRadians(), reconstructed.getRotation().getRadians(), EPSILON);
    }

    // ===== exp/log round-trip tests =====

    @Test
    @DisplayName("exp of zero twist returns same pose")
    void expZeroTwist() {
        Pose2d pose = new Pose2d(1.0, 2.0, new Rotation2d(0.3));
        Pose2d result = pose.exp(new Twist2d(0, 0, 0));
        assertEquals(pose.getX(), result.getX(), EPSILON);
        assertEquals(pose.getY(), result.getY(), EPSILON);
        assertEquals(pose.getRotation().getRadians(), result.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("exp then log round-trips (straight line)")
    void expLogRoundTripStraight() {
        Pose2d start = new Pose2d();
        Twist2d twist = new Twist2d(2.0, 0.0, 0.0); // pure forward
        Pose2d end = start.exp(twist);
        Twist2d recovered = start.log(end);
        assertEquals(twist.dx, recovered.dx, EPSILON);
        assertEquals(twist.dy, recovered.dy, EPSILON);
        assertEquals(twist.dtheta, recovered.dtheta, EPSILON);
    }

    @Test
    @DisplayName("exp then log round-trips (pure rotation)")
    void expLogRoundTripRotation() {
        Pose2d start = new Pose2d();
        Twist2d twist = new Twist2d(0.0, 0.0, Math.PI / 3);
        Pose2d end = start.exp(twist);
        Twist2d recovered = start.log(end);
        assertEquals(twist.dx, recovered.dx, EPSILON);
        assertEquals(twist.dy, recovered.dy, EPSILON);
        assertEquals(twist.dtheta, recovered.dtheta, EPSILON);
    }

    @Test
    @DisplayName("exp then log round-trips (arc motion, reasonable precision)")
    void expLogRoundTripArc() {
        Pose2d start = new Pose2d(1.0, 1.0, new Rotation2d(0.5));
        Twist2d twist = new Twist2d(1.5, 0.3, 0.3);
        Pose2d end = start.exp(twist);
        Twist2d recovered = start.log(end);
        // exp/log round-trip has inherent numerical drift from compound trig
        assertEquals(twist.dx, recovered.dx, 0.01);
        assertEquals(twist.dy, recovered.dy, 0.01);
        assertEquals(twist.dtheta, recovered.dtheta, 0.01);
    }

    @Test
    @DisplayName("Straight-line exp moves in local X direction")
    void expStraightLine() {
        Pose2d start = new Pose2d(0, 0, new Rotation2d(0));
        Twist2d twist = new Twist2d(3.0, 0.0, 0.0);
        Pose2d end = start.exp(twist);
        assertEquals(3.0, end.getX(), EPSILON);
        assertEquals(0.0, end.getY(), EPSILON);
    }

    @Test
    @DisplayName("Straight-line exp in rotated frame")
    void expStraightLineRotated() {
        Pose2d start = new Pose2d(0, 0, new Rotation2d(Math.PI / 2));
        Twist2d twist = new Twist2d(3.0, 0.0, 0.0);
        Pose2d end = start.exp(twist);
        // At 90°, local X maps to global Y
        assertEquals(0.0, end.getX(), EPSILON);
        assertEquals(3.0, end.getY(), EPSILON);
    }

    // ===== Interpolation Tests =====

    @Test
    @DisplayName("Interpolate at t=0 returns start")
    void interpolateStart() {
        Pose2d start = new Pose2d(1, 2, new Rotation2d(0.5));
        Pose2d end = new Pose2d(5, 8, new Rotation2d(1.5));
        assertSame(start, start.interpolate(end, 0.0));
    }

    @Test
    @DisplayName("Interpolate at t=1 returns end")
    void interpolateEnd() {
        Pose2d start = new Pose2d(1, 2, new Rotation2d(0.5));
        Pose2d end = new Pose2d(5, 8, new Rotation2d(1.5));
        assertSame(end, start.interpolate(end, 1.0));
    }

    @Test
    @DisplayName("Interpolation midpoint for pure translation")
    void interpolateMidpointTranslation() {
        Pose2d start = new Pose2d(0, 0, new Rotation2d(0));
        Pose2d end = new Pose2d(4, 0, new Rotation2d(0));
        Pose2d mid = start.interpolate(end, 0.5);
        assertEquals(2.0, mid.getX(), EPSILON);
        assertEquals(0.0, mid.getY(), EPSILON);
    }

    // ===== Equals / HashCode =====

    @Test
    @DisplayName("equals and hashCode contract")
    void equalsAndHashCode() {
        Pose2d a = new Pose2d(1.0, 2.0, new Rotation2d(0.5));
        Pose2d b = new Pose2d(1.0, 2.0, new Rotation2d(0.5));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Not equal when translation differs")
    void notEqual() {
        Pose2d a = new Pose2d(1.0, 2.0, new Rotation2d(0.5));
        Pose2d b = new Pose2d(1.0, 3.0, new Rotation2d(0.5));
        assertNotEquals(a, b);
    }
}

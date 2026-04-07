package org.areslib.math.geometry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Transform2d}.
 */
class Transform2dTest {

    private static final double EPSILON = 1e-6;

    @Test
    @DisplayName("Default constructor produces identity transform")
    void defaultConstructor() {
        Transform2d t = new Transform2d();
        assertEquals(0.0, t.getTranslation().getX(), EPSILON);
        assertEquals(0.0, t.getTranslation().getY(), EPSILON);
        assertEquals(0.0, t.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("Construct from two identical poses produces identity")
    void identityFromSamePose() {
        Pose2d pose = new Pose2d(3.0, 4.0, new Rotation2d(1.0));
        Transform2d t = new Transform2d(pose, pose);
        assertEquals(0.0, t.getTranslation().getX(), EPSILON);
        assertEquals(0.0, t.getTranslation().getY(), EPSILON);
        assertEquals(0.0, t.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("Construct from two poses computes relative transform")
    void constructFromPoses() {
        Pose2d a = new Pose2d(0, 0, new Rotation2d(0));
        Pose2d b = new Pose2d(3, 4, new Rotation2d(Math.PI / 2));
        Transform2d t = new Transform2d(a, b);
        assertEquals(3.0, t.getTranslation().getX(), EPSILON);
        assertEquals(4.0, t.getTranslation().getY(), EPSILON);
        assertEquals(Math.PI / 2, t.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("Construct from rotated initial pose")
    void constructFromRotatedPose() {
        Pose2d a = new Pose2d(0, 0, new Rotation2d(Math.PI / 2));
        Pose2d b = new Pose2d(0, 3, new Rotation2d(Math.PI / 2));
        Transform2d t = new Transform2d(a, b);
        // After rotating diff (0,3) by -90°, we get (3, 0)
        assertEquals(3.0, t.getTranslation().getX(), EPSILON);
        assertEquals(0.0, t.getTranslation().getY(), EPSILON);
        assertEquals(0.0, t.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("times scales both translation and rotation")
    void times() {
        Transform2d t = new Transform2d(new Translation2d(2, 4), new Rotation2d(1.0));
        Transform2d scaled = t.times(0.5);
        assertEquals(1.0, scaled.getTranslation().getX(), EPSILON);
        assertEquals(2.0, scaled.getTranslation().getY(), EPSILON);
        assertEquals(0.5, scaled.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("inverse produces the negated transform")
    void inverse() {
        Transform2d t = new Transform2d(new Translation2d(3, 0), new Rotation2d(0));
        Transform2d inv = t.inverse();
        assertEquals(-3.0, inv.getTranslation().getX(), EPSILON);
        assertEquals(0.0, inv.getTranslation().getY(), EPSILON);
        assertEquals(0.0, inv.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("inverse of rotated transform")
    void inverseRotated() {
        Transform2d t = new Transform2d(new Translation2d(1, 0), new Rotation2d(Math.PI / 2));
        Transform2d inv = t.inverse();
        // Inverse rotation is -90°, inverse translation (-1,0) rotated by -90° = (0,1)
        assertEquals(0.0, inv.getTranslation().getX(), EPSILON);
        assertEquals(1.0, inv.getTranslation().getY(), EPSILON);
        assertEquals(-Math.PI / 2, inv.getRotation().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("equals and hashCode contract")
    void equalsAndHashCode() {
        Transform2d a = new Transform2d(new Translation2d(1, 2), new Rotation2d(0.5));
        Transform2d b = new Transform2d(new Translation2d(1, 2), new Rotation2d(0.5));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Not equal when translation differs")
    void notEqual() {
        Transform2d a = new Transform2d(new Translation2d(1, 2), new Rotation2d(0.5));
        Transform2d b = new Transform2d(new Translation2d(1, 3), new Rotation2d(0.5));
        assertNotEquals(a, b);
    }
}

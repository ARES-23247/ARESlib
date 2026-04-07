package org.areslib.math.geometry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Pose3dTest {

    private static final double kEpsilon = 1e-6;

    @Test
    void identityPose() {
        Pose3d p = new Pose3d();
        assertEquals(0.0, p.getX(), kEpsilon);
        assertEquals(0.0, p.getY(), kEpsilon);
        assertEquals(0.0, p.getZ(), kEpsilon);
    }

    @Test
    void transformByIdentity() {
        Pose3d p = new Pose3d(1, 2, 3, new Rotation3d(0, 0, Math.PI / 4));
        Pose3d result = p.transformBy(new Transform3d());
        assertEquals(p.getX(), result.getX(), kEpsilon);
        assertEquals(p.getY(), result.getY(), kEpsilon);
        assertEquals(p.getZ(), result.getZ(), kEpsilon);
    }

    @Test
    void transformByTranslation() {
        Pose3d p = new Pose3d(0, 0, 0, new Rotation3d());
        Transform3d t = new Transform3d(new Translation3d(1, 2, 3), new Rotation3d());
        Pose3d result = p.transformBy(t);
        assertEquals(1.0, result.getX(), kEpsilon);
        assertEquals(2.0, result.getY(), kEpsilon);
        assertEquals(3.0, result.getZ(), kEpsilon);
    }

    @Test
    void transformByWithRotation() {
        // Robot facing 90° yaw, translate 1m forward in robot frame
        Pose3d p = new Pose3d(0, 0, 0, new Rotation3d(0, 0, Math.PI / 2));
        Transform3d t = new Transform3d(new Translation3d(1, 0, 0), new Rotation3d());
        Pose3d result = p.transformBy(t);
        assertEquals(0.0, result.getX(), kEpsilon);  // 1m forward at 90° = +Y
        assertEquals(1.0, result.getY(), kEpsilon);
    }

    @Test
    void relativeToSelf() {
        Pose3d p = new Pose3d(1, 2, 3, new Rotation3d(0.1, 0.2, 0.3));
        Transform3d result = p.relativeTo(p);
        assertEquals(0.0, result.getX(), kEpsilon);
        assertEquals(0.0, result.getY(), kEpsilon);
        assertEquals(0.0, result.getZ(), kEpsilon);
    }

    @Test
    void relativeToRoundTrip() {
        Pose3d a = new Pose3d(1, 2, 3, new Rotation3d(0.1, 0.2, 0.3));
        Pose3d b = new Pose3d(4, 5, 6, new Rotation3d(0.4, 0.5, 0.6));
        Transform3d aToB = b.relativeTo(a);
        Pose3d reconstructed = a.transformBy(aToB);
        assertEquals(b.getX(), reconstructed.getX(), kEpsilon);
        assertEquals(b.getY(), reconstructed.getY(), kEpsilon);
        assertEquals(b.getZ(), reconstructed.getZ(), kEpsilon);
    }

    @Test
    void toPose2d() {
        Pose3d p = new Pose3d(1, 2, 99, new Rotation3d(0, 0, Math.PI / 3));
        Pose2d p2d = p.toPose2d();
        assertEquals(1.0, p2d.getX(), kEpsilon);
        assertEquals(2.0, p2d.getY(), kEpsilon);
        assertEquals(Math.PI / 3, p2d.getRotation().getRadians(), kEpsilon);
    }

    @Test
    void equalsAndHashCode() {
        Pose3d a = new Pose3d(1, 2, 3, new Rotation3d());
        Pose3d b = new Pose3d(1, 2, 3, new Rotation3d());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}

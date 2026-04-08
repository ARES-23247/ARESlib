package org.areslib.math.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class Transform3dTest {

  private static final double kEpsilon = 1e-6;

  @Test
  void identityTransform() {
    Transform3d t = new Transform3d();
    assertEquals(0.0, t.getX(), kEpsilon);
    assertEquals(0.0, t.getY(), kEpsilon);
    assertEquals(0.0, t.getZ(), kEpsilon);
  }

  @Test
  void inverseRoundTrip() {
    Transform3d t = new Transform3d(new Translation3d(1, 2, 3), new Rotation3d(0.1, 0.2, 0.3));
    Transform3d inv = t.inverse();
    Transform3d roundTrip = t.plus(inv);
    assertEquals(0.0, roundTrip.getX(), kEpsilon);
    assertEquals(0.0, roundTrip.getY(), kEpsilon);
    assertEquals(0.0, roundTrip.getZ(), kEpsilon);
  }

  @Test
  void constructFromPoses() {
    Pose3d a = new Pose3d(1, 0, 0, new Rotation3d());
    Pose3d b = new Pose3d(3, 0, 0, new Rotation3d());
    Transform3d t = new Transform3d(a, b);
    assertEquals(2.0, t.getX(), kEpsilon);
    assertEquals(0.0, t.getY(), kEpsilon);
    assertEquals(0.0, t.getZ(), kEpsilon);
  }

  @Test
  void poseFromTransform() {
    Pose3d a = new Pose3d(1, 0, 0, new Rotation3d());
    Pose3d b = new Pose3d(3, 0, 0, new Rotation3d());
    Transform3d t = new Transform3d(a, b);
    Pose3d reconstructed = a.transformBy(t);
    assertEquals(b.getX(), reconstructed.getX(), kEpsilon);
    assertEquals(b.getY(), reconstructed.getY(), kEpsilon);
    assertEquals(b.getZ(), reconstructed.getZ(), kEpsilon);
  }

  @Test
  void equalsAndHashCode() {
    Transform3d a = new Transform3d(new Translation3d(1, 2, 3), new Rotation3d());
    Transform3d b = new Transform3d(new Translation3d(1, 2, 3), new Rotation3d());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}

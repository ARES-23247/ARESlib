package org.areslib.math.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class Translation3dTest {

  private static final double EPSILON = 1e-9;

  @Test
  void defaultIsZero() {
    Translation3d t = new Translation3d();
    assertEquals(0.0, t.getX(), EPSILON);
    assertEquals(0.0, t.getY(), EPSILON);
    assertEquals(0.0, t.getZ(), EPSILON);
  }

  @Test
  void plusAndMinus() {
    Translation3d a = new Translation3d(1, 2, 3);
    Translation3d b = new Translation3d(4, 5, 6);
    Translation3d sum = a.plus(b);
    assertEquals(5.0, sum.getX(), EPSILON);
    assertEquals(7.0, sum.getY(), EPSILON);
    assertEquals(9.0, sum.getZ(), EPSILON);

    Translation3d diff = a.minus(b);
    assertEquals(-3.0, diff.getX(), EPSILON);
  }

  @Test
  void times() {
    Translation3d t = new Translation3d(1, 2, 3);
    Translation3d scaled = t.times(2.0);
    assertEquals(2.0, scaled.getX(), EPSILON);
    assertEquals(4.0, scaled.getY(), EPSILON);
    assertEquals(6.0, scaled.getZ(), EPSILON);
  }

  @Test
  void norm() {
    Translation3d t = new Translation3d(3, 4, 0);
    assertEquals(5.0, t.getNorm(), EPSILON);
  }

  @Test
  void norm3d() {
    Translation3d t = new Translation3d(1, 2, 2);
    assertEquals(3.0, t.getNorm(), EPSILON);
  }

  @Test
  void unaryMinus() {
    Translation3d t = new Translation3d(1, -2, 3);
    Translation3d neg = t.unaryMinus();
    assertEquals(-1.0, neg.getX(), EPSILON);
    assertEquals(2.0, neg.getY(), EPSILON);
    assertEquals(-3.0, neg.getZ(), EPSILON);
  }

  @Test
  void rotateByIdentity() {
    Translation3d t = new Translation3d(1, 2, 3);
    Translation3d rotated = t.rotateBy(new Rotation3d());
    assertEquals(t, rotated);
  }

  @Test
  void rotateBy90Yaw() {
    Translation3d t = new Translation3d(1, 0, 0);
    Rotation3d rot = new Rotation3d(0.0, 0.0, Math.PI / 2.0);
    Translation3d rotated = t.rotateBy(rot);
    assertEquals(0.0, rotated.getX(), 1e-6);
    assertEquals(1.0, rotated.getY(), 1e-6);
    assertEquals(0.0, rotated.getZ(), 1e-6);
  }

  @Test
  void toTranslation2d() {
    Translation3d t = new Translation3d(1, 2, 99);
    Translation2d t2d = t.toTranslation2d();
    assertEquals(1.0, t2d.getX(), EPSILON);
    assertEquals(2.0, t2d.getY(), EPSILON);
  }

  @Test
  void equalsAndHashCode() {
    Translation3d a = new Translation3d(1, 2, 3);
    Translation3d b = new Translation3d(1, 2, 3);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}

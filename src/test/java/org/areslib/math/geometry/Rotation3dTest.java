package org.areslib.math.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class Rotation3dTest {

  private static final double EPSILON = 1e-6;

  @Test
  void identityRotation() {
    Rotation3d r = new Rotation3d();
    assertEquals(1.0, r.getW(), EPSILON);
    assertEquals(0.0, r.getX(), EPSILON);
    assertEquals(0.0, r.getY(), EPSILON);
    assertEquals(0.0, r.getZ(), EPSILON);
  }

  @Test
  void eulerAnglesRoundTrip() {
    double roll = 0.3;
    double pitch = 0.5;
    double yaw = 1.2;
    Rotation3d r = new Rotation3d(roll, pitch, yaw);
    assertEquals(roll, r.getRoll(), EPSILON);
    assertEquals(pitch, r.getPitch(), EPSILON);
    assertEquals(yaw, r.getYaw(), EPSILON);
  }

  @Test
  void pureYaw() {
    Rotation3d r = new Rotation3d(0.0, 0.0, Math.PI / 2.0);
    assertEquals(0.0, r.getRoll(), EPSILON);
    assertEquals(0.0, r.getPitch(), EPSILON);
    assertEquals(Math.PI / 2.0, r.getYaw(), EPSILON);
  }

  @Test
  void quaternionNormalization() {
    // Non-unit quaternion should be normalized
    Rotation3d r = new Rotation3d(2.0, 0.0, 0.0, 0.0);
    assertEquals(1.0, r.getW(), EPSILON);
    assertEquals(0.0, r.getX(), EPSILON);
  }

  @Test
  void rotateByIdentity() {
    Rotation3d r = new Rotation3d(0.0, 0.0, Math.PI / 4.0);
    Rotation3d result = r.rotateBy(new Rotation3d());
    assertEquals(r, result);
  }

  @Test
  void rotateByComposition() {
    Rotation3d a = new Rotation3d(0.0, 0.0, Math.PI / 2.0); // 90° yaw
    Rotation3d b = new Rotation3d(0.0, 0.0, Math.PI / 2.0); // another 90°
    Rotation3d result = a.rotateBy(b);
    assertEquals(Math.PI, Math.abs(result.getYaw()), EPSILON); // 180° total
  }

  @Test
  void unaryMinusIsInverse() {
    Rotation3d r = new Rotation3d(0.1, 0.2, 0.3);
    Rotation3d identity = r.rotateBy(r.unaryMinus());
    assertEquals(0.0, identity.getRoll(), EPSILON);
    assertEquals(0.0, identity.getPitch(), EPSILON);
    assertEquals(0.0, identity.getYaw(), EPSILON);
  }

  @Test
  void toRotation2dExtractsYaw() {
    double yaw = 1.5;
    Rotation3d r = new Rotation3d(0.0, 0.0, yaw);
    Rotation2d r2d = r.toRotation2d();
    assertEquals(yaw, r2d.getRadians(), EPSILON);
  }

  @Test
  void equalsHandlesQuaternionSign() {
    Rotation3d a = new Rotation3d(1.0, 0.0, 0.0, 0.0);
    Rotation3d b = new Rotation3d(-1.0, 0.0, 0.0, 0.0); // same rotation
    assertEquals(a, b);
  }

  @Test
  void degenerateQuaternion() {
    Rotation3d r = new Rotation3d(0.0, 0.0, 0.0, 0.0);
    assertEquals(1.0, r.getW(), EPSILON); // defaults to identity
  }
}

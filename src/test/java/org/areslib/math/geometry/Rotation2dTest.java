package org.areslib.math.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Rotation2d}. */
class Rotation2dTest {

  private static final double EPSILON = 1e-9;

  @Test
  @DisplayName("Default constructor produces identity (0 rad)")
  void defaultConstructor() {
    Rotation2d rot = new Rotation2d();
    assertEquals(0.0, rot.getRadians(), EPSILON);
    assertEquals(1.0, rot.getCos(), EPSILON);
    assertEquals(0.0, rot.getSin(), EPSILON);
  }

  @Test
  @DisplayName("Construct from radians")
  void constructFromRadians() {
    Rotation2d rot = new Rotation2d(Math.PI / 2);
    assertEquals(Math.PI / 2, rot.getRadians(), EPSILON);
    assertEquals(0.0, rot.getCos(), 1e-6);
    assertEquals(1.0, rot.getSin(), 1e-6);
  }

  @Test
  @DisplayName("Construct from x, y components")
  void constructFromComponents() {
    Rotation2d rot = new Rotation2d(1.0, 1.0);
    assertEquals(Math.PI / 4, rot.getRadians(), EPSILON);
  }

  @Test
  @DisplayName("Construct from near-zero vector defaults to identity")
  void constructFromNearZeroVector() {
    Rotation2d rot = new Rotation2d(1e-7, 1e-7);
    assertEquals(0.0, rot.getRadians(), EPSILON);
    assertEquals(1.0, rot.getCos(), EPSILON);
  }

  @Test
  @DisplayName("fromDegrees factory")
  void fromDegrees() {
    Rotation2d rot = Rotation2d.fromDegrees(90);
    assertEquals(Math.PI / 2, rot.getRadians(), 1e-6);
    assertEquals(90.0, rot.getDegrees(), 1e-6);
  }

  @Test
  @DisplayName("plus combines rotations correctly")
  void plus() {
    Rotation2d a = new Rotation2d(Math.PI / 4);
    Rotation2d b = new Rotation2d(Math.PI / 4);
    Rotation2d result = a.plus(b);
    assertEquals(Math.PI / 2, result.getRadians(), 1e-6);
  }

  @Test
  @DisplayName("minus subtracts rotations correctly")
  void minus() {
    Rotation2d a = new Rotation2d(Math.PI);
    Rotation2d b = new Rotation2d(Math.PI / 2);
    Rotation2d result = a.minus(b);
    assertEquals(Math.PI / 2, result.getRadians(), 1e-6);
  }

  @Test
  @DisplayName("unaryMinus negates the rotation")
  void unaryMinus() {
    Rotation2d rot = new Rotation2d(Math.PI / 3);
    Rotation2d neg = rot.unaryMinus();
    assertEquals(-Math.PI / 3, neg.getRadians(), EPSILON);
  }

  @Test
  @DisplayName("times scales the rotation")
  void times() {
    Rotation2d rot = new Rotation2d(Math.PI / 4);
    Rotation2d scaled = rot.times(2.0);
    assertEquals(Math.PI / 2, scaled.getRadians(), EPSILON);
  }

  @Test
  @DisplayName("rotateBy is equivalent to plus")
  void rotateBy() {
    Rotation2d a = new Rotation2d(Math.PI / 6);
    Rotation2d b = new Rotation2d(Math.PI / 3);
    assertEquals(a.plus(b).getRadians(), a.rotateBy(b).getRadians(), EPSILON);
  }

  @Test
  @DisplayName("Interpolation at boundaries")
  void interpolateBoundaries() {
    Rotation2d start = new Rotation2d(0);
    Rotation2d end = new Rotation2d(Math.PI);
    assertSame(start, start.interpolate(end, 0.0));
    assertSame(end, start.interpolate(end, 1.0));
  }

  @Test
  @DisplayName("Interpolation at midpoint")
  void interpolateMidpoint() {
    Rotation2d start = new Rotation2d(0);
    Rotation2d end = new Rotation2d(Math.PI);
    Rotation2d mid = start.interpolate(end, 0.5);
    assertEquals(Math.PI / 2, mid.getRadians(), 1e-6);
  }

  @Test
  @DisplayName("equals and hashCode contract")
  void equalsAndHashCode() {
    Rotation2d a = new Rotation2d(Math.PI / 4);
    Rotation2d b = new Rotation2d(Math.PI / 4);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  @DisplayName("Near-equal values with epsilon tolerance are equals()")
  void equalsEpsilon() {
    Rotation2d a = new Rotation2d(1.0);
    Rotation2d b = new Rotation2d(1.0 + 1e-10);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  @DisplayName("Not equal beyond epsilon")
  void notEqual() {
    Rotation2d a = new Rotation2d(0);
    Rotation2d b = new Rotation2d(0.1);
    assertNotEquals(a, b);
  }
}

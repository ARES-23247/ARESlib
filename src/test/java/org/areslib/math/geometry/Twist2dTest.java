package org.areslib.math.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Twist2d}. */
class Twist2dTest {

  private static final double EPSILON = 1e-9;

  @Test
  @DisplayName("Default constructor produces zero twist")
  void defaultConstructor() {
    Twist2d twist = new Twist2d();
    assertEquals(0.0, twist.dx, EPSILON);
    assertEquals(0.0, twist.dy, EPSILON);
    assertEquals(0.0, twist.dtheta, EPSILON);
  }

  @Test
  @DisplayName("Parameterized constructor")
  void parameterizedConstructor() {
    Twist2d twist = new Twist2d(1.0, 2.0, 0.5);
    assertEquals(1.0, twist.dx, EPSILON);
    assertEquals(2.0, twist.dy, EPSILON);
    assertEquals(0.5, twist.dtheta, EPSILON);
  }

  @Test
  @DisplayName("scaled multiplies all components")
  void scaled() {
    Twist2d twist = new Twist2d(1.0, 2.0, 0.5);
    Twist2d scaled = twist.scaled(3.0);
    assertEquals(3.0, scaled.dx, EPSILON);
    assertEquals(6.0, scaled.dy, EPSILON);
    assertEquals(1.5, scaled.dtheta, EPSILON);
  }

  @Test
  @DisplayName("scaled by zero produces zero twist")
  void scaledByZero() {
    Twist2d twist = new Twist2d(1.0, 2.0, 0.5);
    Twist2d scaled = twist.scaled(0.0);
    assertEquals(0.0, scaled.dx, EPSILON);
    assertEquals(0.0, scaled.dy, EPSILON);
    assertEquals(0.0, scaled.dtheta, EPSILON);
  }

  @Test
  @DisplayName("scaled by negative inverts direction")
  void scaledByNegative() {
    Twist2d twist = new Twist2d(1.0, 2.0, 0.5);
    Twist2d scaled = twist.scaled(-1.0);
    assertEquals(-1.0, scaled.dx, EPSILON);
    assertEquals(-2.0, scaled.dy, EPSILON);
    assertEquals(-0.5, scaled.dtheta, EPSILON);
  }

  @Test
  @DisplayName("equals and hashCode contract")
  void equalsAndHashCode() {
    Twist2d a = new Twist2d(1.0, 2.0, 0.5);
    Twist2d b = new Twist2d(1.0, 2.0, 0.5);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  @DisplayName("Near-equal values within epsilon are equals()")
  void equalsEpsilon() {
    Twist2d a = new Twist2d(1.0, 2.0, 0.5);
    Twist2d b = new Twist2d(1.0 + 1e-10, 2.0 - 1e-10, 0.5 + 1e-10);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  @DisplayName("Not equal beyond epsilon")
  void notEqual() {
    Twist2d a = new Twist2d(1.0, 2.0, 0.5);
    Twist2d b = new Twist2d(1.0, 2.0, 0.6);
    assertNotEquals(a, b);
  }
}

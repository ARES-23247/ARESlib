package org.areslib.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UnitsTest {

  private static final double EPSILON = 1e-9;

  // ── Length ──────────────────────────────────────────────────────────────────

  @Test
  void inchesToMetersKnownValue() {
    assertEquals(0.0254, Units.inchesToMeters(1.0), EPSILON);
    assertEquals(0.3048, Units.inchesToMeters(12.0), EPSILON); // 1 foot
  }

  @Test
  void metersToInchesKnownValue() {
    assertEquals(1.0, Units.metersToInches(0.0254), EPSILON);
    assertEquals(12.0, Units.metersToInches(0.3048), EPSILON);
  }

  @Test
  void inchesRoundTrip() {
    double original = 17.5;
    assertEquals(original, Units.metersToInches(Units.inchesToMeters(original)), EPSILON);
  }

  @Test
  void feetToMetersKnownValue() {
    assertEquals(0.3048, Units.feetToMeters(1.0), EPSILON);
    assertEquals(0.9144, Units.feetToMeters(3.0), EPSILON); // 1 yard
  }

  @Test
  void feetRoundTrip() {
    double original = 6.0;
    assertEquals(original, Units.metersToFeet(Units.feetToMeters(original)), EPSILON);
  }

  @Test
  void millimetersToMetersKnownValue() {
    assertEquals(0.001, Units.millimetersToMeters(1.0), EPSILON);
    assertEquals(1.0, Units.millimetersToMeters(1000.0), EPSILON);
  }

  @Test
  void millimetersRoundTrip() {
    double original = 435.0;
    assertEquals(original, Units.metersToMillimeters(Units.millimetersToMeters(original)), EPSILON);
  }

  // ── Angle ──────────────────────────────────────────────────────────────────

  @Test
  void degreesToRadiansKnownValues() {
    assertEquals(0.0, Units.degreesToRadians(0.0), EPSILON);
    assertEquals(Math.PI / 2.0, Units.degreesToRadians(90.0), EPSILON);
    assertEquals(Math.PI, Units.degreesToRadians(180.0), EPSILON);
    assertEquals(2.0 * Math.PI, Units.degreesToRadians(360.0), EPSILON);
  }

  @Test
  void angleRoundTrip() {
    double original = 47.5;
    assertEquals(original, Units.radiansToDegrees(Units.degreesToRadians(original)), EPSILON);
  }

  @Test
  void rotationsToRadiansKnownValues() {
    assertEquals(0.0, Units.rotationsToRadians(0.0), EPSILON);
    assertEquals(2.0 * Math.PI, Units.rotationsToRadians(1.0), EPSILON);
    assertEquals(Math.PI, Units.rotationsToRadians(0.5), EPSILON);
  }

  @Test
  void rotationsRoundTrip() {
    double original = 3.75;
    assertEquals(original, Units.radiansToRotations(Units.rotationsToRadians(original)), EPSILON);
  }

  // ── Angular velocity ───────────────────────────────────────────────────────

  @Test
  void rpmToRadPerSecKnownValues() {
    assertEquals(0.0, Units.rpmToRadPerSec(0.0), EPSILON);
    // 60 RPM = 1 rev/sec = 2π rad/s
    assertEquals(2.0 * Math.PI, Units.rpmToRadPerSec(60.0), EPSILON);
  }

  @Test
  void rpmRoundTrip() {
    double original = 5800.0;
    assertEquals(original, Units.radPerSecToRPM(Units.rpmToRadPerSec(original)), EPSILON);
  }

  // ── Edge cases ─────────────────────────────────────────────────────────────

  @Test
  void zeroConversions() {
    assertEquals(0.0, Units.inchesToMeters(0.0), EPSILON);
    assertEquals(0.0, Units.degreesToRadians(0.0), EPSILON);
    assertEquals(0.0, Units.rpmToRadPerSec(0.0), EPSILON);
  }

  @Test
  void negativeConversions() {
    assertEquals(-0.0254, Units.inchesToMeters(-1.0), EPSILON);
    assertEquals(-Math.PI, Units.degreesToRadians(-180.0), EPSILON);
  }
}

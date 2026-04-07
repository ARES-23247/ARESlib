package org.areslib.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnitsTest {

    private static final double kEpsilon = 1e-9;

    // ── Length ──────────────────────────────────────────────────────────────────

    @Test
    void inchesToMetersKnownValue() {
        assertEquals(0.0254, Units.inchesToMeters(1.0), kEpsilon);
        assertEquals(0.3048, Units.inchesToMeters(12.0), kEpsilon); // 1 foot
    }

    @Test
    void metersToInchesKnownValue() {
        assertEquals(1.0, Units.metersToInches(0.0254), kEpsilon);
        assertEquals(12.0, Units.metersToInches(0.3048), kEpsilon);
    }

    @Test
    void inchesRoundTrip() {
        double original = 17.5;
        assertEquals(original, Units.metersToInches(Units.inchesToMeters(original)), kEpsilon);
    }

    @Test
    void feetToMetersKnownValue() {
        assertEquals(0.3048, Units.feetToMeters(1.0), kEpsilon);
        assertEquals(0.9144, Units.feetToMeters(3.0), kEpsilon); // 1 yard
    }

    @Test
    void feetRoundTrip() {
        double original = 6.0;
        assertEquals(original, Units.metersToFeet(Units.feetToMeters(original)), kEpsilon);
    }

    @Test
    void millimetersToMetersKnownValue() {
        assertEquals(0.001, Units.millimetersToMeters(1.0), kEpsilon);
        assertEquals(1.0, Units.millimetersToMeters(1000.0), kEpsilon);
    }

    @Test
    void millimetersRoundTrip() {
        double original = 435.0;
        assertEquals(original, Units.metersToMillimeters(Units.millimetersToMeters(original)), kEpsilon);
    }

    // ── Angle ──────────────────────────────────────────────────────────────────

    @Test
    void degreesToRadiansKnownValues() {
        assertEquals(0.0, Units.degreesToRadians(0.0), kEpsilon);
        assertEquals(Math.PI / 2.0, Units.degreesToRadians(90.0), kEpsilon);
        assertEquals(Math.PI, Units.degreesToRadians(180.0), kEpsilon);
        assertEquals(2.0 * Math.PI, Units.degreesToRadians(360.0), kEpsilon);
    }

    @Test
    void angleRoundTrip() {
        double original = 47.5;
        assertEquals(original, Units.radiansToDegrees(Units.degreesToRadians(original)), kEpsilon);
    }

    @Test
    void rotationsToRadiansKnownValues() {
        assertEquals(0.0, Units.rotationsToRadians(0.0), kEpsilon);
        assertEquals(2.0 * Math.PI, Units.rotationsToRadians(1.0), kEpsilon);
        assertEquals(Math.PI, Units.rotationsToRadians(0.5), kEpsilon);
    }

    @Test
    void rotationsRoundTrip() {
        double original = 3.75;
        assertEquals(original, Units.radiansToRotations(Units.rotationsToRadians(original)), kEpsilon);
    }

    // ── Angular velocity ───────────────────────────────────────────────────────

    @Test
    void rpmToRadPerSecKnownValues() {
        assertEquals(0.0, Units.rpmToRadPerSec(0.0), kEpsilon);
        // 60 RPM = 1 rev/sec = 2π rad/s
        assertEquals(2.0 * Math.PI, Units.rpmToRadPerSec(60.0), kEpsilon);
    }

    @Test
    void rpmRoundTrip() {
        double original = 5800.0;
        assertEquals(original, Units.radPerSecToRPM(Units.rpmToRadPerSec(original)), kEpsilon);
    }

    // ── Edge cases ─────────────────────────────────────────────────────────────

    @Test
    void zeroConversions() {
        assertEquals(0.0, Units.inchesToMeters(0.0), kEpsilon);
        assertEquals(0.0, Units.degreesToRadians(0.0), kEpsilon);
        assertEquals(0.0, Units.rpmToRadPerSec(0.0), kEpsilon);
    }

    @Test
    void negativeConversions() {
        assertEquals(-0.0254, Units.inchesToMeters(-1.0), kEpsilon);
        assertEquals(-Math.PI, Units.degreesToRadians(-180.0), kEpsilon);
    }
}

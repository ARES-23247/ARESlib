package org.areslib.math.geometry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Translation2d}.
 */
class Translation2dTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("Default constructor produces origin")
    void defaultConstructor() {
        Translation2d t = new Translation2d();
        assertEquals(0.0, t.getX(), EPSILON);
        assertEquals(0.0, t.getY(), EPSILON);
    }

    @Test
    @DisplayName("Construct from x, y")
    void constructFromXY() {
        Translation2d t = new Translation2d(3.0, 4.0);
        assertEquals(3.0, t.getX(), EPSILON);
        assertEquals(4.0, t.getY(), EPSILON);
    }

    @Test
    @DisplayName("Construct from distance and angle")
    void constructFromPolar() {
        Translation2d t = new Translation2d(5.0, new Rotation2d(Math.PI / 2));
        assertEquals(0.0, t.getX(), 1e-6);
        assertEquals(5.0, t.getY(), 1e-6);
    }

    @Test
    @DisplayName("getNorm returns correct magnitude")
    void getNorm() {
        Translation2d t = new Translation2d(3.0, 4.0);
        assertEquals(5.0, t.getNorm(), EPSILON);
    }

    @Test
    @DisplayName("getNorm of zero vector is zero")
    void getNormZero() {
        Translation2d t = new Translation2d();
        assertEquals(0.0, t.getNorm(), EPSILON);
    }

    @Test
    @DisplayName("getAngle returns correct angle")
    void getAngle() {
        Translation2d t = new Translation2d(1.0, 1.0);
        assertEquals(Math.PI / 4, t.getAngle().getRadians(), EPSILON);
    }

    @Test
    @DisplayName("plus adds translations")
    void plus() {
        Translation2d a = new Translation2d(1.0, 2.0);
        Translation2d b = new Translation2d(3.0, 4.0);
        Translation2d result = a.plus(b);
        assertEquals(4.0, result.getX(), EPSILON);
        assertEquals(6.0, result.getY(), EPSILON);
    }

    @Test
    @DisplayName("minus subtracts translations")
    void minus() {
        Translation2d a = new Translation2d(5.0, 7.0);
        Translation2d b = new Translation2d(2.0, 3.0);
        Translation2d result = a.minus(b);
        assertEquals(3.0, result.getX(), EPSILON);
        assertEquals(4.0, result.getY(), EPSILON);
    }

    @Test
    @DisplayName("unaryMinus negates translation")
    void unaryMinus() {
        Translation2d t = new Translation2d(3.0, -4.0);
        Translation2d neg = t.unaryMinus();
        assertEquals(-3.0, neg.getX(), EPSILON);
        assertEquals(4.0, neg.getY(), EPSILON);
    }

    @Test
    @DisplayName("times scales by scalar")
    void times() {
        Translation2d t = new Translation2d(2.0, 3.0);
        Translation2d scaled = t.times(2.5);
        assertEquals(5.0, scaled.getX(), EPSILON);
        assertEquals(7.5, scaled.getY(), EPSILON);
    }

    @Test
    @DisplayName("div divides by scalar")
    void div() {
        Translation2d t = new Translation2d(6.0, 9.0);
        Translation2d divided = t.div(3.0);
        assertEquals(2.0, divided.getX(), EPSILON);
        assertEquals(3.0, divided.getY(), EPSILON);
    }

    @Test
    @DisplayName("rotateBy rotates by 90 degrees")
    void rotateBy90() {
        Translation2d t = new Translation2d(1.0, 0.0);
        Translation2d rotated = t.rotateBy(new Rotation2d(Math.PI / 2));
        assertEquals(0.0, rotated.getX(), 1e-6);
        assertEquals(1.0, rotated.getY(), 1e-6);
    }

    @Test
    @DisplayName("rotateBy 360 degrees returns to original")
    void rotateBy360() {
        Translation2d t = new Translation2d(3.0, 4.0);
        Translation2d rotated = t.rotateBy(new Rotation2d(2 * Math.PI));
        assertEquals(t.getX(), rotated.getX(), 1e-6);
        assertEquals(t.getY(), rotated.getY(), 1e-6);
    }

    @Test
    @DisplayName("Interpolation at t=0 returns start")
    void interpolateStart() {
        Translation2d start = new Translation2d(0, 0);
        Translation2d end = new Translation2d(10, 10);
        assertSame(start, start.interpolate(end, 0.0));
    }

    @Test
    @DisplayName("Interpolation at t=1 returns end")
    void interpolateEnd() {
        Translation2d start = new Translation2d(0, 0);
        Translation2d end = new Translation2d(10, 10);
        assertSame(end, start.interpolate(end, 1.0));
    }

    @Test
    @DisplayName("Interpolation at midpoint")
    void interpolateMidpoint() {
        Translation2d start = new Translation2d(0, 0);
        Translation2d end = new Translation2d(10, 20);
        Translation2d mid = start.interpolate(end, 0.5);
        assertEquals(5.0, mid.getX(), EPSILON);
        assertEquals(10.0, mid.getY(), EPSILON);
    }

    @Test
    @DisplayName("equals and hashCode contract")
    void equalsAndHashCode() {
        Translation2d a = new Translation2d(1.0, 2.0);
        Translation2d b = new Translation2d(1.0, 2.0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Near-equal values within epsilon are equals()")
    void equalsEpsilon() {
        Translation2d a = new Translation2d(1.0, 2.0);
        Translation2d b = new Translation2d(1.0 + 1e-10, 2.0 - 1e-10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Not equal beyond epsilon")
    void notEqual() {
        Translation2d a = new Translation2d(1.0, 2.0);
        Translation2d b = new Translation2d(1.0, 2.1);
        assertNotEquals(a, b);
    }
}

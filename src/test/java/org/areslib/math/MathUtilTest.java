package org.areslib.math;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MathUtil}.
 */
class MathUtilTest {

    private static final double EPSILON = 1e-9;

    // ===== clamp(double) =====

    @Test
    @DisplayName("clamp returns value when within range")
    void clampWithinRange() {
        assertEquals(5.0, MathUtil.clamp(5.0, 0.0, 10.0), EPSILON);
    }

    @Test
    @DisplayName("clamp returns low when value is below")
    void clampBelowRange() {
        assertEquals(0.0, MathUtil.clamp(-5.0, 0.0, 10.0), EPSILON);
    }

    @Test
    @DisplayName("clamp returns high when value is above")
    void clampAboveRange() {
        assertEquals(10.0, MathUtil.clamp(15.0, 0.0, 10.0), EPSILON);
    }

    @Test
    @DisplayName("clamp returns boundary values exactly")
    void clampAtBoundary() {
        assertEquals(0.0, MathUtil.clamp(0.0, 0.0, 10.0), EPSILON);
        assertEquals(10.0, MathUtil.clamp(10.0, 0.0, 10.0), EPSILON);
    }

    // ===== clamp(int) =====

    @Test
    @DisplayName("int clamp works correctly")
    void clampInt() {
        assertEquals(5, MathUtil.clamp(5, 0, 10));
        assertEquals(0, MathUtil.clamp(-1, 0, 10));
        assertEquals(10, MathUtil.clamp(15, 0, 10));
    }

    // ===== applyDeadband =====

    @Test
    @DisplayName("applyDeadband returns zero inside deadband")
    void deadbandInsideBand() {
        assertEquals(0.0, MathUtil.applyDeadband(0.05, 0.1), EPSILON);
        assertEquals(0.0, MathUtil.applyDeadband(-0.05, 0.1), EPSILON);
    }

    @Test
    @DisplayName("applyDeadband returns zero at exactly the deadband")
    void deadbandExactly() {
        assertEquals(0.0, MathUtil.applyDeadband(0.1, 0.1), EPSILON);
    }

    @Test
    @DisplayName("applyDeadband scales output outside deadband")
    void deadbandOutside() {
        // At value=1.0 with deadband=0.1, the result should be maxMagnitude (1.0)
        double result = MathUtil.applyDeadband(1.0, 0.1);
        assertEquals(1.0, result, EPSILON);
    }

    @Test
    @DisplayName("applyDeadband handles negative values")
    void deadbandNegative() {
        double result = MathUtil.applyDeadband(-1.0, 0.1);
        assertEquals(-1.0, result, EPSILON);
    }

    @Test
    @DisplayName("applyDeadband with near-zero deadband passes through")
    void deadbandNearZero() {
        double result = MathUtil.applyDeadband(0.5, 1e-15);
        assertTrue(Math.abs(result - 0.5) < 0.01);
    }

    @Test
    @DisplayName("applyDeadband with custom maxMagnitude")
    void deadbandCustomMax() {
        double result = MathUtil.applyDeadband(12.0, 0.0, 12.0);
        assertEquals(12.0, result, EPSILON);
    }

    // ===== angleModulus =====

    @Test
    @DisplayName("angleModulus wraps positive overflow")
    void angleModulusPositiveOverflow() {
        double result = MathUtil.angleModulus(3 * Math.PI);
        assertEquals(-Math.PI, result, EPSILON);
    }

    @Test
    @DisplayName("angleModulus wraps negative overflow")
    void angleModulusNegativeOverflow() {
        double result = MathUtil.angleModulus(-3 * Math.PI);
        assertEquals(-Math.PI, result, EPSILON);
    }

    @Test
    @DisplayName("angleModulus keeps values in [-PI, PI)")
    void angleModulusInRange() {
        assertEquals(0.0, MathUtil.angleModulus(0.0), EPSILON);
        assertEquals(1.0, MathUtil.angleModulus(1.0), EPSILON);
        assertEquals(-1.0, MathUtil.angleModulus(-1.0), EPSILON);
    }

    @Test
    @DisplayName("angleModulus wraps exactly +PI to -PI (half-open range)")
    void angleModulusPiBoundary() {
        assertEquals(-Math.PI, MathUtil.angleModulus(Math.PI), EPSILON);
    }

    @Test
    @DisplayName("angleModulus wraps 2*PI to 0")
    void angleModulusTwoPi() {
        assertEquals(0.0, MathUtil.angleModulus(2 * Math.PI), EPSILON);
    }

    @Test
    @DisplayName("angleModulus wraps large positive angles")
    void angleModulusLargePositive() {
        double result = MathUtil.angleModulus(100 * Math.PI + 0.5);
        assertEquals(0.5, result, 1e-6);
    }

    @Test
    @DisplayName("angleModulus wraps large negative angles")
    void angleModulusLargeNegative() {
        double result = MathUtil.angleModulus(-100 * Math.PI - 0.5);
        assertEquals(-0.5, result, 1e-6);
    }
}

package org.areslib.math.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SimpleMotorFeedforward}.
 */
class SimpleMotorFeedforwardTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("calculate with zero velocity returns zero")
    void calculateZeroVelocity() {
        SimpleMotorFeedforward ff = new SimpleMotorFeedforward(0.1, 0.5, 0.01);
        double result = ff.calculate(0.0);
        assertEquals(0.0, result, EPSILON);
    }

    @Test
    @DisplayName("calculate returns ks + kv * velocity for positive velocity")
    void calculatePositiveVelocity() {
        SimpleMotorFeedforward ff = new SimpleMotorFeedforward(0.1, 0.5, 0.01);
        double result = ff.calculate(2.0);
        // ks * signum(2) + kv * 2 = 0.1 + 1.0 = 1.1
        assertEquals(1.1, result, EPSILON);
    }

    @Test
    @DisplayName("calculate returns -ks + kv * velocity for negative velocity")
    void calculateNegativeVelocity() {
        SimpleMotorFeedforward ff = new SimpleMotorFeedforward(0.1, 0.5, 0.01);
        double result = ff.calculate(-2.0);
        // -0.1 + 0.5 * (-2) = -0.1 - 1.0 = -1.1
        assertEquals(-1.1, result, EPSILON);
    }

    @Test
    @DisplayName("3-arg calculate includes acceleration term")
    void calculateWithAcceleration() {
        SimpleMotorFeedforward ff = new SimpleMotorFeedforward(0.1, 0.5, 0.2);
        double result = ff.calculate(1.0, 3.0, 0.02);
        // accel = (3 - 1) / 0.02 = 100
        // ks * 1 + kv * 1 + ka * 100 = 0.1 + 0.5 + 20.0 = 20.6
        assertEquals(20.6, result, EPSILON);
    }

    @Test
    @DisplayName("3-arg calculate with dt=0 returns steady-state (no NaN)")
    void calculateZeroDt() {
        SimpleMotorFeedforward ff = new SimpleMotorFeedforward(0.1, 0.5, 0.2);
        double result = ff.calculate(2.0, 4.0, 0.0);
        // Should return ks * signum(2) + kv * 2 = 0.1 + 1.0 = 1.1
        assertEquals(1.1, result, EPSILON);
        assertFalse(Double.isNaN(result), "Should not produce NaN with dt=0");
        assertFalse(Double.isInfinite(result), "Should not produce Infinity with dt=0");
    }

    @Test
    @DisplayName("3-arg calculate with negative dt returns steady-state")
    void calculateNegativeDt() {
        SimpleMotorFeedforward ff = new SimpleMotorFeedforward(0.1, 0.5, 0.2);
        double result = ff.calculate(1.0, 2.0, -0.01);
        // Guard should trigger, returning kv * 1 + ks * 1 = 0.6
        assertEquals(0.6, result, EPSILON);
    }
}

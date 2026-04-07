package org.areslib.math.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SlewRateLimiter}.
 */
class SlewRateLimiterTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("No change returns same value")
    void noChange() {
        SlewRateLimiter limiter = new SlewRateLimiter(10.0);
        double result = limiter.calculate(0.0, 0.02);
        assertEquals(0.0, result, EPSILON);
    }

    @Test
    @DisplayName("Small step within rate limit passes through")
    void smallStepPassesThrough() {
        SlewRateLimiter limiter = new SlewRateLimiter(100.0);
        // Max change in 0.02s = 100 * 0.02 = 2.0
        double result = limiter.calculate(1.0, 0.02);
        assertEquals(1.0, result, EPSILON);
    }

    @Test
    @DisplayName("Large positive step is clamped to rate limit")
    void largePositiveStepClamped() {
        SlewRateLimiter limiter = new SlewRateLimiter(10.0);
        // Starting at 0, requesting 100. Max step = 10 * 0.02 = 0.2
        double result = limiter.calculate(100.0, 0.02);
        assertEquals(0.2, result, EPSILON);
    }

    @Test
    @DisplayName("Large negative step is clamped to rate limit")
    void largeNegativeStepClamped() {
        SlewRateLimiter limiter = new SlewRateLimiter(10.0); // negativeRate = -10
        // Starting at 0, requesting -100. Max neg step = -10 * 0.02 = -0.2
        double result = limiter.calculate(-100.0, 0.02);
        assertEquals(-0.2, result, EPSILON);
    }

    @Test
    @DisplayName("Gradual ramp-up over multiple steps")
    void gradualRampUp() {
        SlewRateLimiter limiter = new SlewRateLimiter(10.0);
        double val = limiter.calculate(100.0, 0.1); // max delta = 1.0
        assertEquals(1.0, val, EPSILON);
        val = limiter.calculate(100.0, 0.1); // max delta = 1.0
        assertEquals(2.0, val, EPSILON);
        val = limiter.calculate(100.0, 0.1);
        assertEquals(3.0, val, EPSILON);
    }

    @Test
    @DisplayName("Asymmetric rate limits (fast up, slow down)")
    void asymmetricRateLimits() {
        SlewRateLimiter limiter = new SlewRateLimiter(100.0, -5.0, 0.0);
        // Fast ramp-up: 100 * 0.1 = 10
        double val = limiter.calculate(50.0, 0.1);
        assertEquals(10.0, val, EPSILON);
        
        // Continue to 50
        limiter.calculate(50.0, 0.1); // 20
        limiter.calculate(50.0, 0.1); // 30
        limiter.calculate(50.0, 0.1); // 40
        val = limiter.calculate(50.0, 0.1); // 50
        assertEquals(50.0, val, EPSILON);
        
        // Now ramp-down: limited to -5 * 0.1 = -0.5 per step
        val = limiter.calculate(0.0, 0.1);
        assertEquals(49.5, val, EPSILON);
    }

    @Test
    @DisplayName("reset bypasses rate limit")
    void reset() {
        SlewRateLimiter limiter = new SlewRateLimiter(1.0);
        limiter.calculate(100.0, 0.02); // clamped
        limiter.reset(50.0);
        double result = limiter.calculate(50.0, 0.02);
        assertEquals(50.0, result, EPSILON); // no change needed
    }

    @Test
    @DisplayName("Zero period produces no change")
    void zeroPeriod() {
        SlewRateLimiter limiter = new SlewRateLimiter(10.0);
        double result = limiter.calculate(100.0, 0.0);
        // max delta = 10 * 0 = 0, so output stays at 0
        assertEquals(0.0, result, EPSILON);
    }

    @Test
    @DisplayName("Custom initial value")
    void customInitialValue() {
        SlewRateLimiter limiter = new SlewRateLimiter(10.0, -10.0, 5.0);
        double result = limiter.calculate(5.0, 0.02);
        assertEquals(5.0, result, EPSILON); // no change needed
    }
}

package org.areslib.math.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ElevatorFeedforward}.
 */
class ElevatorFeedforwardTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("Zero velocity returns gravity term only")
    void zeroVelocityGravity() {
        ElevatorFeedforward ff = new ElevatorFeedforward(0.1, 0.5, 2.0, 0.3);
        double result = ff.calculate(0.0, 0.0);
        // ks * signum(0) + kg + kv * 0 + ka * 0 = 0 + 0.5 = 0.5
        assertEquals(0.5, result, EPSILON);
    }

    @Test
    @DisplayName("Positive velocity adds ks and kv")
    void positiveVelocity() {
        ElevatorFeedforward ff = new ElevatorFeedforward(0.1, 0.5, 2.0, 0.0);
        double result = ff.calculate(3.0);
        // 0.1 * 1 + 0.5 + 2.0 * 3 = 0.1 + 0.5 + 6.0 = 6.6
        assertEquals(6.6, result, EPSILON);
    }

    @Test
    @DisplayName("Negative velocity flips ks sign")
    void negativeVelocity() {
        ElevatorFeedforward ff = new ElevatorFeedforward(0.1, 0.5, 2.0, 0.0);
        double result = ff.calculate(-3.0);
        // -0.1 + 0.5 + -6.0 = -5.6
        assertEquals(-5.6, result, EPSILON);
    }

    @Test
    @DisplayName("Gravity term is always present (unlike arm)")
    void gravityAlwaysPresent() {
        ElevatorFeedforward ff = new ElevatorFeedforward(0.0, 1.0, 0.0, 0.0);
        assertEquals(1.0, ff.calculate(0.0), EPSILON);
        assertEquals(1.0, ff.calculate(5.0) - 0.0 * 5.0, EPSILON); // gravity always 1.0
    }

    @Test
    @DisplayName("Acceleration term included")
    void accelerationTerm() {
        ElevatorFeedforward ff = new ElevatorFeedforward(0.0, 0.0, 0.0, 0.5);
        double result = ff.calculate(0.0, 10.0);
        assertEquals(5.0, result, EPSILON);
    }

    @Test
    @DisplayName("1-arg calculate defaults acceleration to zero")
    void oneArgDefaultsAccel() {
        ElevatorFeedforward ff = new ElevatorFeedforward(0.0, 0.0, 0.0, 100.0);
        double result = ff.calculate(0.0);
        assertEquals(0.0, result, EPSILON);
    }

    @Test
    @DisplayName("3-arg constructor defaults ka to 0")
    void threeArgConstructor() {
        ElevatorFeedforward ff = new ElevatorFeedforward(0.1, 0.5, 2.0);
        assertEquals(0.0, ff.ka, EPSILON);
    }

    @Test
    @DisplayName("All components together")
    void allComponents() {
        ElevatorFeedforward ff = new ElevatorFeedforward(0.1, 0.5, 2.0, 0.3);
        double result = ff.calculate(4.0, 2.0);
        // 0.1 * 1 + 0.5 + 2.0 * 4 + 0.3 * 2 = 0.1 + 0.5 + 8.0 + 0.6 = 9.2
        assertEquals(9.2, result, EPSILON);
    }
}

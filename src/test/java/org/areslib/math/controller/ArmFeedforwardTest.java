package org.areslib.math.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArmFeedforward}.
 */
class ArmFeedforwardTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("Zero velocity and horizontal position gives gravity term only")
    void horizontalGravity() {
        ArmFeedforward ff = new ArmFeedforward(0.1, 0.5, 1.0, 0.01);
        // At 0 rad (horizontal), cos(0) = 1. velocity = 0 → ks*signum(0) = 0
        double result = ff.calculate(0.0, 0.0, 0.0);
        // 0.0 + 0.5 * 1.0 + 0.0 + 0.0 = 0.5
        assertEquals(0.5, result, EPSILON);
    }

    @Test
    @DisplayName("Vertical position (90°) has zero gravity component")
    void verticalNoGravity() {
        ArmFeedforward ff = new ArmFeedforward(0.1, 0.5, 1.0, 0.01);
        double result = ff.calculate(Math.PI / 2, 0.0, 0.0);
        // cos(PI/2) ≈ 0. ks*0 + kg*0 + kv*0 + ka*0 ≈ 0
        assertEquals(0.0, result, 1e-6);
    }

    @Test
    @DisplayName("Positive velocity adds ks and kv terms")
    void positiveVelocity() {
        ArmFeedforward ff = new ArmFeedforward(0.1, 0.0, 2.0, 0.0);
        double result = ff.calculate(0.0, 3.0);
        // ks * 1 + kg * 0 + kv * 3 = 0.1 + 0 + 6.0 = 6.1
        // Wait, kg is 0 but cos(0) = 1 → 0 * 1 = 0
        assertEquals(6.1, result, EPSILON);
    }

    @Test
    @DisplayName("Negative velocity flips ks sign")
    void negativeVelocity() {
        ArmFeedforward ff = new ArmFeedforward(0.1, 0.0, 2.0, 0.0);
        double result = ff.calculate(0.0, -3.0);
        // ks * -1 + kv * -3 = -0.1 + -6.0 = -6.1
        assertEquals(-6.1, result, EPSILON);
    }

    @Test
    @DisplayName("Acceleration term included with 3-arg calculate")
    void accelerationTerm() {
        ArmFeedforward ff = new ArmFeedforward(0.0, 0.0, 0.0, 0.5);
        double result = ff.calculate(0.0, 0.0, 10.0);
        // Only ka * 10 = 5.0
        assertEquals(5.0, result, EPSILON);
    }

    @Test
    @DisplayName("2-arg calculate defaults acceleration to zero")
    void twoArgDefaultsAccel() {
        ArmFeedforward ff = new ArmFeedforward(0.0, 0.0, 0.0, 100.0);
        double result = ff.calculate(0.0, 0.0);
        assertEquals(0.0, result, EPSILON);
    }

    @Test
    @DisplayName("All components together")
    void allComponents() {
        ArmFeedforward ff = new ArmFeedforward(0.1, 0.5, 2.0, 0.3);
        double result = ff.calculate(Math.PI / 3, 4.0, 2.0);
        // ks * 1 + kg * cos(60°) + kv * 4 + ka * 2
        // 0.1 + 0.5 * 0.5 + 8.0 + 0.6 = 0.1 + 0.25 + 8.0 + 0.6 = 8.95
        assertEquals(8.95, result, EPSILON);
    }

    @Test
    @DisplayName("3-arg constructor defaults ka to 0")
    void threeArgConstructor() {
        ArmFeedforward ff = new ArmFeedforward(0.1, 0.5, 2.0);
        assertEquals(0.0, ff.ka, EPSILON);
    }
}

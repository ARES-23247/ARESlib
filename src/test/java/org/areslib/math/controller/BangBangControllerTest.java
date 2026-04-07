package org.areslib.math.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BangBangController}.
 */
class BangBangControllerTest {

    private static final double EPSILON = 1e-9;

    @Test
    @DisplayName("Output is 1.0 when below setpoint")
    void outputWhenBelowSetpoint() {
        BangBangController controller = new BangBangController();
        double output = controller.calculate(5.0, 10.0);
        assertEquals(1.0, output, EPSILON);
    }

    @Test
    @DisplayName("Output is 0.0 when above setpoint")
    void outputWhenAboveSetpoint() {
        BangBangController controller = new BangBangController();
        double output = controller.calculate(15.0, 10.0);
        assertEquals(0.0, output, EPSILON);
    }

    @Test
    @DisplayName("Output is 0.0 within tolerance")
    void outputWithinTolerance() {
        BangBangController controller = new BangBangController(1.0);
        double output = controller.calculate(9.5, 10.0);
        // error = 0.5, tolerance = 1.0, so within tolerance => 0.0
        assertEquals(0.0, output, EPSILON);
    }

    @Test
    @DisplayName("atSetpoint returns true within tolerance")
    void atSetpointWithinTolerance() {
        BangBangController controller = new BangBangController(1.0);
        controller.calculate(9.5, 10.0);
        assertTrue(controller.atSetpoint(9.5));
    }

    @Test
    @DisplayName("atSetpoint returns false outside tolerance")
    void atSetpointOutsideTolerance() {
        BangBangController controller = new BangBangController(1.0);
        controller.calculate(5.0, 10.0);
        assertFalse(controller.atSetpoint(5.0));
    }

    @Test
    @DisplayName("atSetpoint returns true at exactly the tolerance boundary")
    void atSetpointExactlyAtBoundary() {
        BangBangController controller = new BangBangController(1.0);
        controller.calculate(9.0, 10.0);
        // error = 1.0 = tolerance, should be true (<=)
        assertTrue(controller.atSetpoint(9.0));
    }

}


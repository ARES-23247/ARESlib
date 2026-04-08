package org.areslib.math.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PIDController}. */
class PIDControllerTest {

  private static final double EPSILON = 1e-6;

  @Test
  @DisplayName("P-only controller produces proportional output")
  void pOnlyController() {
    PIDController pid = new PIDController(2.0, 0.0, 0.0, 0.02);
    double output = pid.calculate(0.0, 10.0);
    // error = 10.0, kP = 2.0 => output = 20.0
    assertEquals(20.0, output, EPSILON);
  }

  @Test
  @DisplayName("At setpoint produces zero output (P-only)")
  void atSetpointPOnly() {
    PIDController pid = new PIDController(1.0, 0.0, 0.0, 0.02);
    double output = pid.calculate(5.0, 5.0);
    assertEquals(0.0, output, EPSILON);
  }

  @Test
  @DisplayName("Integral accumulates over time")
  void integralAccumulates() {
    PIDController pid = new PIDController(0.0, 1.0, 0.0, 0.02);
    pid.calculate(0.0, 10.0); // error = 10, integral = 10 * 0.02 = 0.2
    double output2 = pid.calculate(0.0, 10.0); // integral = 0.2 + 0.2 = 0.4
    assertEquals(0.4, output2, EPSILON);
  }

  @Test
  @DisplayName("Derivative responds to error change")
  void derivativeResponds() {
    PIDController pid = new PIDController(0.0, 0.0, 1.0, 0.02);
    pid.calculate(0.0, 10.0); // error = 10, prevError was 0
    double output2 = pid.calculate(5.0, 10.0); // error = 5, deriv = (5-10)/0.02 = -250
    assertEquals(-250.0, output2, EPSILON);
  }

  @Test
  @DisplayName("Reset clears integral and previous error")
  void resetClearsState() {
    PIDController pid = new PIDController(0.0, 1.0, 0.0, 0.02);
    pid.calculate(0.0, 10.0);
    pid.calculate(0.0, 10.0);
    pid.reset();
    double output = pid.calculate(0.0, 10.0);
    // After reset, integral should be fresh: 10 * 0.02 = 0.2
    assertEquals(0.2, output, EPSILON);
  }

  @Test
  @DisplayName("Continuous input wraps error correctly")
  void continuousInput() {
    PIDController pid = new PIDController(1.0, 0.0, 0.0, 0.02);
    pid.enableContinuousInput(-Math.PI, Math.PI);
    // Setpoint = -3.0, measurement = 3.0
    // Naive error = -6.0, but wrapped it should be ~0.28 (shorter path)
    double output = pid.calculate(3.0, -3.0);
    // The wrapping should produce a small error rather than -6.0
    assertTrue(
        Math.abs(output) < 1.0, "Continuous input should wrap the error to the shorter path");
  }

  @Test
  @DisplayName("Zero period prevents divide-by-zero in derivative")
  void zeroPeriodGuard() {
    PIDController pid = new PIDController(1.0, 0.0, 1.0, 0.0);
    // Should not throw, derivative should be 0
    double output = pid.calculate(0.0, 10.0);
    // kP * 10 + kI * 0 + kD * 0 = 10
    assertEquals(10.0, output, EPSILON);
  }
}

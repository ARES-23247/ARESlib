package org.areslib.math.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link OnlineFeedforwardEstimator}. */
class OnlineFeedforwardEstimatorTest {

  @Test
  void testConstructor() {
    OnlineFeedforwardEstimator estimator = new OnlineFeedforwardEstimator("Test", 100, 0.1);
    assertEquals("Test", estimator.getName());
    assertEquals(0, estimator.getSampleCount());
    assertFalse(estimator.hasReliableEstimates());
    assertEquals(0.0, estimator.getEstimatedKV());
    assertEquals(0.0, estimator.getEstimatedKA());
  }

  @Test
  void testBasicFunctionality() {
    OnlineFeedforwardEstimator estimator = new OnlineFeedforwardEstimator("Test", 100, 0.1);

    // Feed some realistic data points
    // Simulating a mechanism with kV=0.5, kA=0.2, kS=0.1
    for (int i = 0; i < 20; i++) {
      double velocity = 1.0 + (i * 0.1); // Varying velocity
      double acceleration = 0.5; // Constant acceleration
      double voltage = 0.1 + 0.5 * velocity + 0.2 * acceleration; // Expected voltage
      estimator.addMeasurement(voltage, velocity, acceleration);
    }

    // After enough samples, we should have reliable estimates
    assertTrue(estimator.hasReliableEstimates());
    assertTrue(estimator.getSampleCount() >= 10);

    // The estimates should be close to the true values (kV=0.5, kA=0.2)
    // Allow some tolerance due to OLS regression
    assertTrue(Math.abs(estimator.getEstimatedKV() - 0.5) < 0.3);
    assertTrue(Math.abs(estimator.getEstimatedKA() - 0.2) < 0.3);
  }

  @Test
  void testLowVelocityFiltering() {
    OnlineFeedforwardEstimator estimator = new OnlineFeedforwardEstimator("Test", 100, 0.1);

    // Feed low velocity data (should be filtered out)
    for (int i = 0; i < 20; i++) {
      estimator.addMeasurement(1.0, 0.05, 0.1); // Velocity < 0.1 threshold
    }

    // Should not have collected any samples
    assertEquals(0, estimator.getSampleCount());
    assertFalse(estimator.hasReliableEstimates());
  }

  @Test
  void testReset() {
    OnlineFeedforwardEstimator estimator = new OnlineFeedforwardEstimator("Test", 100, 0.1);

    // Add some data
    for (int i = 0; i < 20; i++) {
      estimator.addMeasurement(1.0, 1.0, 0.5);
    }

    assertTrue(estimator.getSampleCount() > 0);

    // Reset
    estimator.reset();

    // Should be back to initial state
    assertEquals(0, estimator.getSampleCount());
    assertFalse(estimator.hasReliableEstimates());
    assertEquals(0.0, estimator.getEstimatedKV());
    assertEquals(0.0, estimator.getEstimatedKA());
  }

  @Test
  void testSlidingWindow() {
    int windowSize = 10;
    OnlineFeedforwardEstimator estimator = new OnlineFeedforwardEstimator("Test", windowSize, 0.1);

    // Add more data than window size
    for (int i = 0; i < 30; i++) {
      double velocity = 1.0 + (i * 0.1);
      double acceleration = 0.5;
      double voltage = 0.1 + 0.5 * velocity + 0.2 * acceleration;
      estimator.addMeasurement(voltage, velocity, acceleration);
    }

    // Should not exceed window size
    assertTrue(estimator.getSampleCount() <= windowSize);
  }

  @Test
  void testStaticFrictionCompensation() {
    OnlineFeedforwardEstimator estimator =
        new OnlineFeedforwardEstimator("Test", 100, 0.2); // kS = 0.2

    // Feed data that accounts for static friction
    for (int i = 0; i < 20; i++) {
      double velocity = 1.0;
      double acceleration = 0.0;
      double appliedVoltage = 0.2 + 0.5 * velocity; // kS + kV*velocity
      estimator.addMeasurement(appliedVoltage, velocity, acceleration);
    }

    // Should have reliable estimates
    assertTrue(estimator.hasReliableEstimates());

    // kV should be close to 0.5 (the true value)
    assertTrue(Math.abs(estimator.getEstimatedKV() - 0.5) < 0.3);
  }

  @Test
  void testNegativeVelocities() {
    OnlineFeedforwardEstimator estimator = new OnlineFeedforwardEstimator("Test", 100, 0.1);

    // Feed data with negative velocities
    for (int i = 0; i < 20; i++) {
      double velocity = -1.0 - (i * 0.1); // Negative velocities
      double acceleration = -0.5;
      double voltage = -0.1 + 0.5 * velocity + 0.2 * acceleration;
      estimator.addMeasurement(voltage, velocity, acceleration);
    }

    // Should handle negative velocities correctly
    assertTrue(estimator.hasReliableEstimates());
    assertTrue(Math.abs(estimator.getEstimatedKV() - 0.5) < 0.3);
  }
}

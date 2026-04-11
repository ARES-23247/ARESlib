package org.areslib.math.filter;

import java.util.ArrayDeque;

/**
 * This class implements a linear, time-invariant filter. Common uses include moving-average filters
 * or low-pass filters.
 */
public class LinearFilter {
  private final int size;
  private final ArrayDeque<Double> inputs;

  /**
   * Creates a moving average filter.
   *
   * @param size The size of the moving average.
   * @return A linear filter that computes a moving average.
   */
  public static LinearFilter movingAverage(int size) {
    return new LinearFilter(size);
  }

  private LinearFilter(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Filter size must be strictly positive");
    }
    this.size = size;
    inputs = new ArrayDeque<>(size + 1);
  }

  /**
   * Calculates the next value of the filter.
   *
   * @param input The current measurement.
   * @return The filtered value.
   */
  public double calculate(double input) {
    inputs.addFirst(input);
    if (inputs.size() > size) {
      inputs.removeLast();
    }

    double sum = 0.0;
    for (double val : inputs) {
      sum += val;
    }

    return sum / inputs.size();
  }

  /** Resets the filter to an empty state. */
  public void reset() {
    inputs.clear();
  }
}

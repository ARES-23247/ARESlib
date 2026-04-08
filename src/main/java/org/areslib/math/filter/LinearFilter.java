package org.areslib.math.filter;

import java.util.LinkedList;

/**
 * This class implements a linear, time-invariant filter. Common uses include moving-average filters
 * or low-pass filters.
 */
public class LinearFilter {
  private final int m_size;
  private final LinkedList<Double> m_inputs;

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
    m_size = size;
    m_inputs = new LinkedList<>();
  }

  /**
   * Calculates the next value of the filter.
   *
   * @param input The current measurement.
   * @return The filtered value.
   */
  public double calculate(double input) {
    m_inputs.addFirst(input);
    if (m_inputs.size() > m_size) {
      m_inputs.removeLast();
    }

    double sum = 0.0;
    for (double val : m_inputs) {
      sum += val;
    }

    return sum / m_inputs.size();
  }

  /** Resets the filter to an empty state. */
  public void reset() {
    m_inputs.clear();
  }
}

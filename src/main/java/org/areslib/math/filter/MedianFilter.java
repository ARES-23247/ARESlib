package org.areslib.math.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A class that implements a moving-window median filter. Useful for reducing measurement noise,
 * especially for robust signal isolation from large out-of-band spikes (like LiDAR readings).
 */
public class MedianFilter {
  private final int m_size;
  private final LinkedList<Double> m_valueBuffer;

  /**
   * Creates a new MedianFilter.
   *
   * @param size The number of samples in the moving window.
   */
  public MedianFilter(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Filter size must be strictly positive");
    }
    m_size = size;
    m_valueBuffer = new LinkedList<>();
  }

  /**
   * Calculates the moving-window median for the next value of the input stream.
   *
   * @param next The next input value.
   * @return The median of the moving window, updated to include the next value.
   */
  public double calculate(double next) {
    m_valueBuffer.add(next);

    if (m_valueBuffer.size() > m_size) {
      m_valueBuffer.removeFirst();
    }

    List<Double> sortedBuffer = new ArrayList<>(m_valueBuffer);
    Collections.sort(sortedBuffer);

    int size = sortedBuffer.size();
    if (size % 2 == 1) {
      return sortedBuffer.get(size / 2);
    } else {
      return (sortedBuffer.get(size / 2 - 1) + sortedBuffer.get(size / 2)) / 2.0;
    }
  }

  /** Resets the filter, clearing the window of all elements. */
  public void reset() {
    m_valueBuffer.clear();
  }
}

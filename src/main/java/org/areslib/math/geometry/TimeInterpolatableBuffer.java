package org.areslib.math.geometry;

import org.areslib.math.filter.InterpolatingTreeMap;

/**
 * A buffer that stores a history of objects that implement Interpolatable. Allows lookup of values
 * at any given point in time (interpolated if exact timestamp isn't found).
 *
 * <p>Commonly used for Vision Latency Compensation (keeping a history of Pose2d).
 */
public class TimeInterpolatableBuffer<T extends Interpolatable<T>> {
  private final InterpolatingTreeMap<Double, T> m_buffer;

  private TimeInterpolatableBuffer(int historySize) {
    m_buffer = new InterpolatingTreeMap<>(historySize);
  }

  /**
   * Create a new TimeInterpolatableBuffer.
   *
   * @param historySize max samples to store.
   * @param <T> Type of interpolatable.
   * @return the buffer.
   */
  public static <T extends Interpolatable<T>> TimeInterpolatableBuffer<T> createBuffer(
      int historySize) {
    return new TimeInterpolatableBuffer<>(historySize);
  }

  /**
   * Adds an observation to the buffer.
   *
   * @param timeSeconds The timestamp of the observation.
   * @param sample The observation.
   */
  public void addSample(double timeSeconds, T sample) {
    m_buffer.put(timeSeconds, sample);
  }

  /**
   * Retrieves the interpolated sample at the given timestamp.
   *
   * @param timeSeconds The timestamp to look up.
   * @return The interpolated sample, or null if the buffer is empty.
   */
  public T getSample(double timeSeconds) {
    return m_buffer.get(timeSeconds);
  }

  /** Clears the buffer. */
  public void clear() {
    m_buffer.clear();
  }
}

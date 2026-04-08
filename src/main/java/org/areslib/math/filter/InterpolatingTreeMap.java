package org.areslib.math.filter;

import java.util.Map;
import java.util.TreeMap;
import org.areslib.math.geometry.Interpolatable;

/**
 * A TreeMap that automatically interpolates between values if the exact key is not found.
 *
 * <p>Used for pose history buffers and vision latency compensations.
 *
 * @param <K> The key type, usually Double representing a timestamp.
 * @param <V> The value type, must implement Interpolatable.
 */
public class InterpolatingTreeMap<K extends Number, V extends Interpolatable<V>> {
  private final TreeMap<Double, V> m_map = new TreeMap<>();
  private final int m_maxSize;

  /**
   * @param maxSize The maximum number of entries to store.
   */
  public InterpolatingTreeMap(int maxSize) {
    m_maxSize = maxSize;
  }

  /**
   * Put a value into the map.
   *
   * @param key Timestamp.
   * @param value Value at the timestamp.
   */
  public void put(K key, V value) {
    m_map.put(key.doubleValue(), value);
    if (m_map.size() > m_maxSize) {
      m_map.remove(m_map.firstKey());
    }
  }

  /** Clears all elements from the map. */
  public void clear() {
    m_map.clear();
  }

  /**
   * Retrieves the value at the given key. If the key doesn't exist, it will interpolate between the
   * closest lower and higher keys.
   *
   * @param key Timestamp.
   * @return The interpolated value, or null if the map is empty.
   */
  public V get(K key) {
    double dKey = key.doubleValue();
    if (m_map.isEmpty()) {
      return null;
    }

    V exact = m_map.get(dKey);
    if (exact != null) {
      return exact;
    }

    Map.Entry<Double, V> lower = m_map.floorEntry(dKey);
    Map.Entry<Double, V> upper = m_map.ceilingEntry(dKey);

    if (lower == null) {
      return upper.getValue();
    }
    if (upper == null) {
      return lower.getValue();
    }

    // Interpolate between the two
    double timeDelta = upper.getKey() - lower.getKey();
    if (timeDelta <= 0.0) {
      return lower.getValue();
    }

    double targetDelta = dKey - lower.getKey();
    double t = targetDelta / timeDelta;

    return lower.getValue().interpolate(upper.getValue(), t);
  }
}

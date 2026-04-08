package org.areslib.math.geometry;

/**
 * An interface for objects that can be interpolated between each other.
 *
 * @param <T> The type of the object.
 */
public interface Interpolatable<T> {
  /**
   * Interpolates between this value and another value.
   *
   * @param endValue The end value to interpolate towards.
   * @param t The percentage to interpolate [0.0, 1.0]. 0.0 returns this value, 1.0 returns the
   *     endValue.
   * @return The interpolated value.
   */
  T interpolate(T endValue, double t);
}

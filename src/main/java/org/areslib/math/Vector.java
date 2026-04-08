package org.areslib.math;

/**
 * Vector standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code Vector}.
 * Extracted and compiled as part of the ARESLib2 Code Audit for missing documentation coverage.
 */
public class Vector<N> {
  private final double[] data;

  public Vector() {
    this.data = new double[0];
  }

  public Vector(double... data) {
    this.data = data;
  }

  public double get(int index) {
    return data[index];
  }
}

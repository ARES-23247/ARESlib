package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android
 * dependencies.
 */
import org.areslib.math.Vector;
import org.areslib.math.numbers.N2;
import org.areslib.math.numbers.N3;

public class VecBuilder {
  public static Vector<N2> fill(double d1, double d2) {
    return new Vector<>(d1, d2);
  }

  public static Vector<N3> fill(double d1, double d2, double d3) {
    return new Vector<>(d1, d2, d3);
  }
}

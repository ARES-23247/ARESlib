package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android dependencies.
 */
import org.areslib.math.Vector;
import org.areslib.math.numbers.N2;
public class VecBuilder {
    public static Vector<N2> fill(double d1, double d2) { return new Vector<>(); }
}
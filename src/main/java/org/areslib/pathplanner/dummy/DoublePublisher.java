package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android
 * dependencies.
 */
public class DoublePublisher {
  public void set(double d) {}
}

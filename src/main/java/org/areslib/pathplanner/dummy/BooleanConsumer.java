package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android
 * dependencies.
 */
public interface BooleanConsumer {
  void accept(boolean value);
}

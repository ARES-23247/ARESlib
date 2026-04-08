package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android
 * dependencies.
 */
public class NetworkTableInstance {
  public static NetworkTableInstance getDefault() {
    return new NetworkTableInstance();
  }

  public org.areslib.pathplanner.dummy.StructPublisher getStructTopic(String a, String b) {
    return new org.areslib.pathplanner.dummy.StructPublisher();
  }

  public org.areslib.pathplanner.dummy.StructArrayPublisher getStructArrayTopic(
      String a, String b) {
    return new org.areslib.pathplanner.dummy.StructArrayPublisher();
  }

  public org.areslib.pathplanner.dummy.NetworkTable getTable(String name) {
    return new org.areslib.pathplanner.dummy.NetworkTable();
  }
}

package org.areslib.pathplanner.dummy;

/** A dummy NetworkTable to allow PathPlanner configuration without NT4 dependencies. */
public class NetworkTable {
  public GenericEntry getEntry(String key) {
    return new GenericEntry();
  }
}

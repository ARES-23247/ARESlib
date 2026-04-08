package org.areslib.pathplanner.dummy;

/** A dummy RobotBase mocking execution modes. */
public class RobotBase {
  public static boolean isReal() {
    return false;
  }

  public static boolean isSimulation() {
    return true;
  }
}

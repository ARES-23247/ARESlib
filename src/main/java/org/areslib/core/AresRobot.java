package org.areslib.core;

/** Global utility class representing robot state. */
public class AresRobot {
  private static boolean isSimulation = false;

  /**
   * Standard loop execution period for the robot (20ms) used in controllers and simulation physics.
   */
  public static final double LOOP_PERIOD_SECS = 0.02;

  /**
   * @return True if the robot is currently running in a simulated environment.
   */
  public static boolean isSimulation() {
    return isSimulation;
  }

  /**
   * Sets the simulation state. This is typically invoked by desktop launchers like
   * DesktopSimLauncher.
   *
   * @param sim true if running in simulation
   */
  public static void setSimulation(boolean sim) {
    isSimulation = sim;
  }
}

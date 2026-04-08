package org.areslib.pathplanner.dummy;

import java.util.Optional;

/**
 * A dummy implementation of the WPILib DriverStation. This class abstracts away the FRC
 * DriverStation dependency and hardcodes standard simulation responses (e.g. Always Enabled, Always
 * Autonomous) so that PathPlanner can execute natively in the ARESLib2 headless environment.
 */
public class DriverStation {
  /** Dummy representation of an FRC Alliance color. */
  public static class Alliance {
    public static final Alliance Red = new Alliance();
    public static final Alliance Blue = new Alliance();
  }

  /**
   * Gets the expected alliance color.
   *
   * @return hardcoded Blue alliance for simulation stability.
   */
  public static Optional<Alliance> getAlliance() {
    return Optional.of(Alliance.Blue);
  }

  /**
   * Reports a warning to the console.
   *
   * @param msg the warning message
   * @param p print trace
   */
  public static void reportWarning(String msg, boolean p) {
    System.out.println("WARNING: " + msg);
  }

  /**
   * Reports an error to the console.
   *
   * @param msg the error message
   * @param p print trace
   */
  public static void reportError(String msg, boolean p) {
    System.err.println("ERROR: " + msg);
  }

  /**
   * Check if the robot is enabled.
   *
   * @return true always (simulated enabled)
   */
  public static boolean isEnabled() {
    return true;
  }

  /**
   * Check if the robot is in autonomous.
   *
   * @return true always (simulated auto execution)
   */
  public static boolean isAutonomous() {
    return true;
  }

  /**
   * Check if the robot is in teleop.
   *
   * @return false always
   */
  public static boolean isTeleop() {
    return false;
  }
}

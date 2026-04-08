package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android
 * dependencies. Now routes calls to the internal ARESLib logger.
 */
public class SmartDashboard {
  public static void putData(String key, Sendable data) {}

  public static void putBoolean(String key, boolean value) {
    org.areslib.telemetry.AresAutoLogger.recordOutput(key, value ? 1.0 : 0.0);
  }

  public static void putNumber(String key, double value) {
    org.areslib.telemetry.AresAutoLogger.recordOutput(key, value);
  }

  public static void putString(String key, String value) {
    org.areslib.telemetry.AresAutoLogger.recordOutput(key, value);
  }
}

package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android dependencies.
 */
public class SmartDashboard {
    public static void putData(String key, Sendable data) {}
    public static void putBoolean(String key, boolean value) {}
    public static void putNumber(String key, double value) {}
    public static void putString(String key, String value) {}
}
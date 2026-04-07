package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android dependencies.
 */
public class MathSharedStore { public static double getTimestamp() { return System.nanoTime() / 1e9; } public static void reportUsage(int a, int b) {} }
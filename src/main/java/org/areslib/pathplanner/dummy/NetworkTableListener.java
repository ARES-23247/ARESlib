package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android dependencies.
 */
public class NetworkTableListener { public static NetworkTableListener createListener(org.areslib.pathplanner.dummy.NetworkTable t, java.util.EnumSet<NetworkTableEvent.Kind> k, java.util.function.Consumer<NetworkTableEvent> c) { return new NetworkTableListener(); } public void close() {} }
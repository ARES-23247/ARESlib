package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android dependencies.
 */
public class GenericPublisher {
    public void setBoolean(boolean value) {}
    public void setDouble(double value) {}
    public void setString(String value) {}
}
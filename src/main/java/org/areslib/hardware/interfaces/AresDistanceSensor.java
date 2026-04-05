package org.areslib.hardware.interfaces;

/**
 * Hardware-agnostic interface for an analog or digital distance sensor.
 * Standardizes time-of-flight (ToF) or ultrasonic readings into physical metric units.
 */
public interface AresDistanceSensor {
    /**
     * Gets the measured distance in meters.
     * @return distance in meters
     */
    double getDistanceMeters();
}

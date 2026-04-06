package org.areslib.hardware.interfaces;

import org.areslib.telemetry.AresLoggableInputs;

/**
 * AdvantageKit-style IO abstraction for a Time-of-Flight (ToF) array lidar sensor.
 * Useful for mapping multiple zones (e.g. 4x4 or 8x8) of distance data.
 */
public interface ArrayLidarIO {

    /**
     * Loggable data object containing the zones from the Lidar array.
     */
    class ArrayLidarInputs implements AresLoggableInputs {
        /** Automatically sized by the backend wrapper (e.g., 16 for 4x4, 64 for 8x8). Array of distances in millimeters. */
        public double[] distanceZonesMm = new double[0]; 
    }

    /**
     * Updates the data structure with the latest array zone values from the underlying hardware sensor or simulation.
     * 
     * @param inputs The ArrayLidarInputs object to be populated.
     */
    void updateInputs(ArrayLidarInputs inputs);
}

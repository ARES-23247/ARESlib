package org.areslib.hardware.sensors;

import org.areslib.telemetry.AresLoggableInputs;

public interface ArrayLidarIO {

    public static class ArrayLidarInputs implements AresLoggableInputs {
        // Automatically sized by the backend wrapper (e.g., 16 for 4x4, 64 for 8x8)
        public double[] distanceZonesMm = new double[0]; 
    }

    void updateInputs(ArrayLidarInputs inputs);
}

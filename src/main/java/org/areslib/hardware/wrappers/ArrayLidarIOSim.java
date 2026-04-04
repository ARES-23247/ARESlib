package org.areslib.hardware.wrappers;

import org.areslib.hardware.sensors.ArrayLidarIO;

public class ArrayLidarIOSim implements ArrayLidarIO {

    private final int resolution;

    /** Create a simulation configured for 16 (4x4) or 64 (8x8) resolution */
    public ArrayLidarIOSim(int resolution) {
        this.resolution = resolution;
    }

    /** Defaults to standard full 64 (8x8) resolution */
    public ArrayLidarIOSim() {
        this.resolution = 64;
    }

    @Override
    public void updateInputs(ArrayLidarInputs inputs) {
        if (inputs.distanceZonesMm.length != resolution) {
            inputs.distanceZonesMm = new double[resolution];
        }

        // Create a 10-second simulation visual loop
        long loopTimeMs = System.currentTimeMillis() % 10000;
        
        // Calculate dynamic wall distance: 2000mm down to 100mm
        double simulatedDistanceMm = 2000.0 - (1900.0 * (loopTimeMs / 10000.0));

        // Assign the simulated flat wall distance to all zones
        for (int i = 0; i < resolution; i++) {
            inputs.distanceZonesMm[i] = simulatedDistanceMm;
        }
    }
}

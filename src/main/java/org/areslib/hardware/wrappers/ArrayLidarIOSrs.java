package org.areslib.hardware.wrappers;

import org.areslib.hardware.interfaces.ArrayLidarIO;
import org.areslib.hardware.AresHardwareManager;

import java.lang.reflect.Method;

public class ArrayLidarIOSrs implements ArrayLidarIO {
    
    private final int port;
    private final Object srsHub;
    private Method getLidarZoneArrayMethod;

    public ArrayLidarIOSrs(int port) {
        if (port < 0 || port > 2) {
            throw new IllegalArgumentException("SRS Hub I2C port must be 0, 1, or 2.");
        }
        this.port = port;
        this.srsHub = AresHardwareManager.getActiveSrsHub();

        if (srsHub != null) {
            try {
                // Dynamically map to the SRS Hub's native Array LiDAR fetching method.
                // Assuming "getLidarZones" or "getVl53l5cxDistances" that returns a primitive array.
                Class<?> clazz = srsHub.getClass();
                
                try {
                    this.getLidarZoneArrayMethod = clazz.getMethod("getLidarZones", int.class);
                } catch (Exception e) {
                    try {
                        this.getLidarZoneArrayMethod = clazz.getMethod("getVl53l5cxDistances", int.class);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void updateInputs(ArrayLidarInputs inputs) {
        if (getLidarZoneArrayMethod != null) {
            try {
                Object result = getLidarZoneArrayMethod.invoke(srsHub, port);
                
                // Copy the SRS bulk-read 64-zone map cleanly into our ArrayLidarInputs struct in 0.0ms.
                if (result instanceof double[]) {
                    double[] arr = (double[]) result;
                    if (inputs.distanceZonesMm.length != arr.length) {
                        inputs.distanceZonesMm = new double[arr.length]; // GC pause only ONCE per hardware flip
                    }
                    System.arraycopy(arr, 0, inputs.distanceZonesMm, 0, arr.length);
                } else if (result instanceof short[]) {
                    short[] arr = (short[]) result;
                    if (inputs.distanceZonesMm.length != arr.length) {
                        inputs.distanceZonesMm = new double[arr.length];
                    }
                    for (int i = 0; i < arr.length; i++) {
                        inputs.distanceZonesMm[i] = arr[i];
                    }
                } else if (result instanceof int[]) {
                    int[] arr = (int[]) result;
                    if (inputs.distanceZonesMm.length != arr.length) {
                        inputs.distanceZonesMm = new double[arr.length];
                    }
                    for (int i = 0; i < arr.length; i++) {
                        inputs.distanceZonesMm[i] = arr[i];
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}

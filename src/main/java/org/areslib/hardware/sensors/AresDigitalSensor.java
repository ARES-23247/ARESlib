package org.areslib.hardware.sensors;

public interface AresDigitalSensor {
    /**
     * Gets the boolean state of the digital sensor.
     * @return true if high, false if low
     */
    boolean getState();
}

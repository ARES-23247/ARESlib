package org.areslib.hardware.sensors;

/**
 * Hardware-agnostic interface for an analog sensor.
 * Standardizes raw voltage reads from physical analog pins or simulated sensors.
 */
public interface AresAnalogSensor {
    /**
     * Gets the current voltage reading of the analog sensor.
     * @return the voltage (typically 0.0 to 3.3 or 5.0)
     */
    double getVoltage();
}

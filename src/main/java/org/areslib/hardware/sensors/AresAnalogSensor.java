package org.areslib.hardware.sensors;

public interface AresAnalogSensor {
    /**
     * Gets the current voltage reading of the analog sensor.
     * @return the voltage (typically 0.0 to 3.3 or 5.0)
     */
    double getVoltage();
}

package org.areslib.hardware.sensors;

/**
 * Hardware-agnostic interface for a digital sensor (e.g., limit switch, touch sensor).
 * Provides a universal method to obtain a boolean state regardless of the underlying 
 * hardware platform (FTC SDK, simulated digital IO, or coprocessor inputs).
 */
public interface AresDigitalSensor {
    /**
     * Gets the boolean state of the digital sensor.
     * @return true if high, false if low
     */
    boolean getState();
}

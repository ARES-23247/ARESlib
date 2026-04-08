package org.areslib.hardware.interfaces;

public interface AresDigitalSensor {
  /**
   * Gets the boolean state of the digital sensor.
   *
   * @return true if triggered/high, false otherwise.
   */
  boolean getState();
}

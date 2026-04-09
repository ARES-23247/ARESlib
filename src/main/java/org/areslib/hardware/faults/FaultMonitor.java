package org.areslib.hardware.faults;

/**
 * Interface that hardware components can implement to self-report faults to the global
 * RobotHealthTracker subsystem.
 */
public interface FaultMonitor {

  /**
   * Returns whether a critical hardware fault is currently active.
   *
   * @return True if a critical hardware fault is currently active.
   */
  boolean hasHardwareFault();

  /**
   * Returns details regarding the hardware fault.
   *
   * @return Details regarding the hardware fault (e.g. "I2C Disconnected", "Motor Controller
   *     Voltage Under 6V", etc.)
   */
  String getFaultMessage();
}

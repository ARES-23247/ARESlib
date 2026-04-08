package org.areslib.hardware.interfaces;

/**
 * Hardware-agnostic interface for a standard PWM Servo. Used for manipulating robot appendages and
 * linear actuators.
 */
public interface AresServo {
  /**
   * Sets the position of the servo.
   *
   * @param position A value typically between 0.0 and 1.0.
   */
  void setPosition(double position);
}

package org.areslib.hardware.wrappers;

import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.interfaces.AresAbsoluteEncoder;
import org.areslib.hardware.interfaces.AresAnalogSensor;
import org.areslib.hardware.interfaces.AresDigitalSensor;

/**
 * Smart Adapter for the SRS Hub coprocessor. Dynamically configures the hardware pin and routes
 * data from the active driver.
 */
public class AresSrsSensor implements AresAbsoluteEncoder, AresAnalogSensor, AresDigitalSensor {

  private final org.areslib.hardware.coprocessors.SRSHub activeSrsHub;
  private final int port;
  private double offset = 0.0;

  public AresSrsSensor(int port, SrsMode mode) {
    this.port = port;
    this.activeSrsHub = AresHardwareManager.getActiveSrsHub();
  }

  @Override
  public void setDistancePerPulse(double distance) {}

  @Override
  public double getPosition() {
    if (activeSrsHub == null) return 0.0;
    try {
      return activeSrsHub.readEncoder(port).position;
    } catch (Exception e) {
      return 0.0;
    }
  }

  @Override
  public double getVelocity() {
    if (activeSrsHub == null) return 0.0;
    try {
      return activeSrsHub.readEncoder(port).velocity;
    } catch (Exception e) {
      return 0.0;
    }
  }

  @Override
  public void setOffset(double offset) {
    this.offset = offset;
  }

  @Override
  public double getAbsolutePositionRad() {
    return Math.toRadians(getPosition()) - offset;
  }

  @Override
  public double getVoltage() {
    if (activeSrsHub == null) return 0.0;
    try {
      // SRSHub returns a 0.0 to 1.0 ratio, multiply by 3.3 for standard analog voltage
      return activeSrsHub.readAnalogDigitalDevice(port) * 3.3;
    } catch (Exception e) {
      return 0.0;
    }
  }

  @Override
  public boolean getState() {
    if (activeSrsHub == null) return false;
    try {
      return activeSrsHub.readAnalogDigitalDevice(port) > 0.5;
    } catch (Exception e) {
      return false;
    }
  }
}

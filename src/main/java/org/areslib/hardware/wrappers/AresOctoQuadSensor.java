package org.areslib.hardware.wrappers;

import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.coprocessors.AresOctoQuadDriver;
import org.areslib.hardware.coprocessors.OctoMode;
import org.areslib.hardware.interfaces.AresAbsoluteEncoder;

/**
 * Smart Adapter for the OctoQuad coprocessor. Dynamically configures the hardware pin and routes
 * data from the active driver.
 */
public class AresOctoQuadSensor implements AresAbsoluteEncoder {

  private final AresOctoQuadDriver driver;
  private final int channel;
  private final OctoMode mode;
  private double offset = 0.0;

  public AresOctoQuadSensor(int channel, OctoMode mode) {
    this.channel = channel;
    this.mode = mode;
    this.driver = AresHardwareManager.getActiveOctoQuad();

    if (this.driver != null) {
      this.driver.setChannelBankConfig(channel, mode);
    } else {
      com.qualcomm.robotcore.util.RobotLog.i(
          "AresOctoQuadSensor: Hardware not present (simulation or disabled)");
    }
  }

  @Override
  public void setDistancePerPulse(double distance) {
    // OctoQuad usually returns raw ticks or pulse width, scaling applied at subsystem level
    // typically
  }

  @Override
  public double getPosition() {
    if (driver == null) return 0.0;

    switch (mode) {
      case ENCODER:
        return driver.readPosition(channel);
      case PULSE_WIDTH:
      case ABSOLUTE:
        return driver.readPulseWidth(channel);
      default:
        return 0.0;
    }
  }

  @Override
  public double getVelocity() {
    // Velocity requires either a velocity SDK method or delta-time derivation.
    // Assuming 0 for now as OctoQuad SDK wrapper currently only defines readPosition/readPulseWidth
    return 0.0;
  }

  @Override
  public void setOffset(double offset) {
    this.offset = offset;
  }

  @Override
  public double getAbsolutePositionRad() {
    if (driver == null) return 0.0;

    if (mode == OctoMode.ABSOLUTE || mode == OctoMode.PULSE_WIDTH) {
      // Pulse width typically ranges based on sensor, standard 3.3V PWM absolute is often 0-3.32ms
      // (1-1024 us) mapped to 2pi
      // Usually, getPosition() returns microseconds. Convert standard 1024 us period to 2PI
      // radians.
      // Note: This relies on the specific absolute encoder pulse behavior.
      double pulseUsec = driver.readPulseWidth(channel);
      double rad = (pulseUsec / 1024.0) * 2 * Math.PI;
      return org.areslib.math.MathUtil.angleModulus(rad - offset);
    } else {
      return org.areslib.math.MathUtil.angleModulus(getPosition() - offset);
    }
  }
}

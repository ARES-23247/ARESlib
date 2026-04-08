package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.HardwareMap;
import java.lang.reflect.Method;
import org.areslib.hardware.interfaces.AresAbsoluteEncoder;

/** Optional Wrapper for the SRSHub driver using reflection. */
public class SrsHubEncoderWrapper implements AresAbsoluteEncoder {

  private final Object srsHubDevice;
  private final int channelIndex;
  private final Method getEncoderPositionMethod;
  private final Method getEncoderVelocityMethod;

  public SrsHubEncoderWrapper(HardwareMap hardwareMap, String deviceName, int channel) {
    this.channelIndex = channel;
    try {
      this.srsHubDevice = hardwareMap.get(deviceName);
      Class<?> clazz = srsHubDevice.getClass();
      this.getEncoderPositionMethod = clazz.getMethod("getEncoderPosition", int.class);
      this.getEncoderVelocityMethod = clazz.getMethod("getEncoderVelocity", int.class);
    } catch (Exception e) {
      throw new RuntimeException(
          "ARESlib: Failed to bind to SRSHub driver. Make sure the driver is installed.", e);
    }
  }

  @Override
  public void setDistancePerPulse(double distance) {}

  @Override
  public double getPosition() {
    try {
      return ((Number) getEncoderPositionMethod.invoke(srsHubDevice, channelIndex)).doubleValue();
    } catch (Exception e) {
      throw new RuntimeException("ARESlib: SRSHub getEncoderPosition failed.", e);
    }
  }

  @Override
  public double getVelocity() {
    try {
      return ((Number) getEncoderVelocityMethod.invoke(srsHubDevice, channelIndex)).doubleValue();
    } catch (Exception e) {
      throw new RuntimeException("ARESlib: SRSHub getEncoderVelocity failed.", e);
    }
  }

  private double offset = 0.0;

  @Override
  public void setOffset(double offset) {
    this.offset = offset;
  }

  @Override
  public double getAbsolutePositionRad() {
    // SRS Hub returns degrees usually, adapt to radians. If units are raw, this will need scaling
    // tuning.
    return Math.toRadians(getPosition()) - offset;
  }
}

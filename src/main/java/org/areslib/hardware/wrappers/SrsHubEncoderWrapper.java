package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;
import java.lang.reflect.Method;
import org.areslib.hardware.interfaces.AresAbsoluteEncoder;

/** Optional Wrapper for the SRSHub driver using reflection. */
public class SrsHubEncoderWrapper implements AresAbsoluteEncoder {

  private Object srsHubDevice;
  private int channelIndex;
  private Method getEncoderPositionMethod;
  private Method getEncoderVelocityMethod;

  public SrsHubEncoderWrapper(HardwareMap hardwareMap, String deviceName, int channel) {
    this.channelIndex = channel;
    try {
      this.srsHubDevice = hardwareMap.get(deviceName);
      Class<?> clazz = srsHubDevice.getClass();
      this.getEncoderPositionMethod = clazz.getMethod("getEncoderPosition", int.class);
      this.getEncoderVelocityMethod = clazz.getMethod("getEncoderVelocity", int.class);
    } catch (Exception e) {
      RobotLog.addGlobalWarningMessage(
          "ARESlib: Failed to bind to SRSHub driver. Make sure the driver is installed. "
              + e.getMessage());
    }
  }

  @Override
  public void setDistancePerPulse(double distance) {}

  @Override
  public double getPosition() {
    try {
      if (getEncoderPositionMethod != null) {
        return ((Number) getEncoderPositionMethod.invoke(srsHubDevice, channelIndex)).doubleValue();
      }
    } catch (Exception e) {
      RobotLog.addGlobalWarningMessage(
          "ARESlib: SRSHub getEncoderPosition failed: " + e.getMessage());
    }
    return 0.0;
  }

  @Override
  public double getVelocity() {
    try {
      if (getEncoderVelocityMethod != null) {
        return ((Number) getEncoderVelocityMethod.invoke(srsHubDevice, channelIndex)).doubleValue();
      }
    } catch (Exception e) {
      RobotLog.addGlobalWarningMessage(
          "ARESlib: SRSHub getEncoderVelocity failed: " + e.getMessage());
    }
    return 0.0;
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

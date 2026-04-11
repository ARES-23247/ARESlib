package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;
import java.lang.reflect.Method;
import org.areslib.hardware.interfaces.AresEncoder;

/** Optional Wrapper for the DigitalChickenLabs OctoQuad driver using reflection. */
public class OctoQuadEncoderWrapper implements AresEncoder {

  private Object octoQuadDevice;
  private int channelIndex;
  private Method getSinglePositionMethod;
  private Method getSingleVelocityMethod;

  public OctoQuadEncoderWrapper(HardwareMap hardwareMap, String deviceName, int channel) {
    this.channelIndex = channel;
    try {
      this.octoQuadDevice = hardwareMap.get(deviceName);
      Class<?> clazz = octoQuadDevice.getClass();
      this.getSinglePositionMethod = clazz.getMethod("getSinglePosition", int.class);
      this.getSingleVelocityMethod = clazz.getMethod("getSingleVelocity", int.class);
    } catch (Exception e) {
      RobotLog.addGlobalWarningMessage(
          "ARESlib: Failed to bind to OctoQuad driver. Make sure the driver is installed. "
              + e.getMessage());
    }
  }

  @Override
  public void setDistancePerPulse(double distance) {}

  @Override
  public double getPosition() {
    try {
      if (getSinglePositionMethod != null) {
        return ((Number) getSinglePositionMethod.invoke(octoQuadDevice, channelIndex))
            .doubleValue();
      }
    } catch (Exception e) {
      RobotLog.addGlobalWarningMessage(
          "ARESlib: OctoQuad getSinglePosition failed: " + e.getMessage());
    }
    return 0.0;
  }

  @Override
  public double getVelocity() {
    try {
      if (getSingleVelocityMethod != null) {
        return ((Number) getSingleVelocityMethod.invoke(octoQuadDevice, channelIndex))
            .doubleValue();
      }
    } catch (Exception e) {
      RobotLog.addGlobalWarningMessage(
          "ARESlib: OctoQuad getSingleVelocity failed: " + e.getMessage());
    }
    return 0.0;
  }
}

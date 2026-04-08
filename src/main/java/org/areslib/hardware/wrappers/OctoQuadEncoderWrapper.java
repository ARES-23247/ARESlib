package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.HardwareMap;
import java.lang.reflect.Method;
import org.areslib.hardware.interfaces.AresEncoder;

/** Optional Wrapper for the DigitalChickenLabs OctoQuad driver using reflection. */
public class OctoQuadEncoderWrapper implements AresEncoder {

  private final Object octoQuadDevice;
  private final int channelIndex;
  private final Method getSinglePositionMethod;
  private final Method getSingleVelocityMethod;

  public OctoQuadEncoderWrapper(HardwareMap hardwareMap, String deviceName, int channel) {
    this.channelIndex = channel;
    try {
      this.octoQuadDevice = hardwareMap.get(deviceName);
      Class<?> clazz = octoQuadDevice.getClass();
      this.getSinglePositionMethod = clazz.getMethod("getSinglePosition", int.class);
      this.getSingleVelocityMethod = clazz.getMethod("getSingleVelocity", int.class);
    } catch (Exception e) {
      throw new RuntimeException(
          "ARESlib: Failed to bind to OctoQuad driver. Make sure the driver is installed.", e);
    }
  }

  @Override
  public void setDistancePerPulse(double distance) {}

  @Override
  public double getPosition() {
    try {
      return ((Number) getSinglePositionMethod.invoke(octoQuadDevice, channelIndex)).doubleValue();
    } catch (Exception e) {
      throw new RuntimeException("ARESlib: OctoQuad getSinglePosition failed.", e);
    }
  }

  @Override
  public double getVelocity() {
    try {
      return ((Number) getSingleVelocityMethod.invoke(octoQuadDevice, channelIndex)).doubleValue();
    } catch (Exception e) {
      throw new RuntimeException("ARESlib: OctoQuad getSingleVelocity failed.", e);
    }
  }
}

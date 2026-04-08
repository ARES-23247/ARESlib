package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.HardwareMap;
import java.lang.reflect.Method;
import org.areslib.hardware.faults.FaultMonitor;
import org.areslib.hardware.interfaces.OdometryIO;

/**
 * An optional wrapper for the GoBilda Pinpoint driver. Uses Reflection to prevent ARESlib from
 * having a hard dependency on the app module's teamcode.
 */
public class PinpointOdometryWrapper implements OdometryIO, FaultMonitor {

  private final Object pinpointDevice;
  private final Method updateMethod;
  private final Method getPosXMethod;
  private final Method getPosYMethod;
  private final Method getHeadingMethod;
  private final Method getVelXMethod;
  private final Method getVelYMethod;
  private final Method getHeadingVelocityMethod;
  private boolean faultTripped = false;
  private String faultDetail = "";

  public PinpointOdometryWrapper(HardwareMap hardwareMap, String deviceName) {
    try {
      this.pinpointDevice = hardwareMap.get(deviceName);
      Class<?> clazz = pinpointDevice.getClass();
      this.updateMethod = clazz.getMethod("update");
      this.getPosXMethod = clazz.getMethod("getPosX");
      this.getPosYMethod = clazz.getMethod("getPosY");
      this.getHeadingMethod = clazz.getMethod("getHeading");
      this.getVelXMethod = clazz.getMethod("getVelX");
      this.getVelYMethod = clazz.getMethod("getVelY");
      this.getHeadingVelocityMethod = clazz.getMethod("getHeadingVelocity");
    } catch (Exception e) {
      throw new RuntimeException(
          "ARESlib: Failed to bind to GoBilda Pinpoint driver using Reflection. Ensure the driver is installed.",
          e);
    }
  }

  /**
   * Updates the pinpoint's internal odometry calculation. Must be called before fetching pose
   * natively or through loop.
   */
  public void update() {
    try {
      updateMethod.invoke(pinpointDevice);
    } catch (Exception e) {
      faultTripped = true;
      faultDetail = "Pinpoint update() failed: " + e.getMessage();
    }
  }

  @Override
  public void updateInputs(OdometryInputs inputs) {
    try {
      updateMethod.invoke(pinpointDevice);

      // ENFORCE WPILIB COORDINATE CONVENTIONS
      // FTC Standard: X = Right, Y = Forward
      // WPILib Standard: X = Forward, Y = Left
      inputs.xMeters =
          org.areslib.core.CoordinateUtil.mmToMeters((double) getPosYMethod.invoke(pinpointDevice));
      inputs.yMeters =
          -org.areslib.core.CoordinateUtil.mmToMeters(
              (double) getPosXMethod.invoke(pinpointDevice));
      inputs.headingRadians = (double) getHeadingMethod.invoke(pinpointDevice);

      inputs.xVelocityMetersPerSecond =
          org.areslib.core.CoordinateUtil.mmToMeters((double) getVelYMethod.invoke(pinpointDevice));
      inputs.yVelocityMetersPerSecond =
          -org.areslib.core.CoordinateUtil.mmToMeters(
              (double) getVelXMethod.invoke(pinpointDevice));
      inputs.angularVelocityRadiansPerSecond =
          (double) getHeadingVelocityMethod.invoke(pinpointDevice);

      // If we get here, communications are healthy
      faultTripped = false;
    } catch (Exception e) {
      faultTripped = true;
      faultDetail = "Pinpoint I2C read failed: " + e.getMessage();
      // Gracefully zero out instead of crashing the scheduler
      inputs.xMeters = 0.0;
      inputs.yMeters = 0.0;
      inputs.headingRadians = 0.0;
      inputs.xVelocityMetersPerSecond = 0.0;
      inputs.yVelocityMetersPerSecond = 0.0;
      inputs.angularVelocityRadiansPerSecond = 0.0;
    }
  }

  @Override
  public boolean hasHardwareFault() {
    return faultTripped;
  }

  @Override
  public String getFaultMessage() {
    return "CRITICAL: GoBilda Pinpoint Odometry Offline — " + faultDetail;
  }
}

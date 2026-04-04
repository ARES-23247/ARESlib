package org.areslib.hardware.wrappers;

import org.areslib.hardware.interfaces.OdometryIO;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.lang.reflect.Method;

/**
 * An optional wrapper for the GoBilda Pinpoint driver.
 * Uses Reflection to prevent ARESlib from having a hard dependency on the app module's teamcode. 
 */
public class PinpointOdometryWrapper implements OdometryIO {

    private final Object pinpointDevice;
    private final Method updateMethod;
    private final Method getPosXMethod;
    private final Method getPosYMethod;
    private final Method getHeadingMethod;
    private final Method getVelXMethod;
    private final Method getVelYMethod;
    private final Method getHeadingVelocityMethod;

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
            throw new RuntimeException("ARESlib: Failed to bind to GoBilda Pinpoint driver using Reflection. Ensure the driver is installed.", e);
        }
    }

    /**
     * Updates the pinpoint's internal odometry calculation. Must be called before fetching pose natively or through loop.
     */
    public void update() {
        try {
            updateMethod.invoke(pinpointDevice);
        } catch (Exception e) {
            throw new RuntimeException("ARESlib: Pinpoint update() failed.", e);
        }
    }

    @Override
    public void updateInputs(OdometryInputs inputs) {
        try {
            updateMethod.invoke(pinpointDevice);
            
            // Assuming the Pinpoint driver's getPosX/Y returns values in millimeters
            inputs.xMeters = ((double) getPosXMethod.invoke(pinpointDevice)) / 1000.0;
            inputs.yMeters = ((double) getPosYMethod.invoke(pinpointDevice)) / 1000.0;
            inputs.headingRadians = (double) getHeadingMethod.invoke(pinpointDevice);
            
            inputs.xVelocityMetersPerSecond = ((double) getVelXMethod.invoke(pinpointDevice)) / 1000.0;
            inputs.yVelocityMetersPerSecond = ((double) getVelYMethod.invoke(pinpointDevice)) / 1000.0;
            inputs.angularVelocityRadiansPerSecond = (double) getHeadingVelocityMethod.invoke(pinpointDevice);
        } catch (Exception e) {
            throw new RuntimeException("ARESlib: Pinpoint getPose() failed.", e);
        }
    }
}

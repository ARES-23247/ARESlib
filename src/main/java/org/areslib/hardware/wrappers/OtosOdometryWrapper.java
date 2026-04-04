package org.areslib.hardware.wrappers;

import com.qualcomm.hardware.sparkfun.SparkFunOTOS;
import org.areslib.hardware.interfaces.OdometryIO;

public class OtosOdometryWrapper implements OdometryIO {
    
    private final SparkFunOTOS otos;

    public OtosOdometryWrapper(SparkFunOTOS otos) {
        this.otos = otos;
    }

    @Override
    public void updateInputs(OdometryInputs inputs) {
        SparkFunOTOS.Pose2D otosPose = otos.getPosition();
        SparkFunOTOS.Pose2D otosVel = otos.getVelocity();

        // OTOS natively uses inches and degrees. Convert to meters and radians.
        inputs.xMeters = otosPose.x * 0.0254;
        inputs.yMeters = otosPose.y * 0.0254;
        inputs.headingRadians = Math.toRadians(otosPose.h);

        inputs.xVelocityMetersPerSecond = otosVel.x * 0.0254;
        inputs.yVelocityMetersPerSecond = otosVel.y * 0.0254;
        inputs.angularVelocityRadiansPerSecond = Math.toRadians(otosVel.h);
    }
}

package org.areslib.hardware.wrappers;

import com.qualcomm.hardware.sparkfun.SparkFunOTOS;
import org.areslib.hardware.interfaces.OdometryIO;

import org.areslib.hardware.faults.FaultMonitor;
import org.areslib.hardware.faults.AresHardwareFaultInjector;

public class OtosOdometryWrapper implements OdometryIO, FaultMonitor {
    
    private final SparkFunOTOS otos;
    private boolean faultTripped = false;

    public OtosOdometryWrapper(SparkFunOTOS otos) {
        this.otos = otos;
    }

    @Override
    public void updateInputs(OdometryInputs inputs) {
        if (AresHardwareFaultInjector.simulateEncoderShatter) {
            faultTripped = true;
            // Overwrite with zeros to simulate severed tracking lines
            inputs.xMeters = 0.0;
            inputs.yMeters = 0.0;
            inputs.headingRadians = 0.0;
            inputs.xVelocityMetersPerSecond = 0.0;
            inputs.yVelocityMetersPerSecond = 0.0;
            inputs.angularVelocityRadiansPerSecond = 0.0;
            return; // Halt standard physical loop updates
        }

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

    @Override
    public boolean hasHardwareFault() {
        return faultTripped;
    }

    @Override
    public String getFaultMessage() {
        return "CRITICAL DISCONNECT: SparkFun OTOS Dead-Wheel Matrix Offline!";
    }
}

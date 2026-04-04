package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.AnalogInput;
import org.areslib.hardware.sensors.AresAbsoluteEncoder;

public class AnalogAbsoluteEncoderWrapper implements AresAbsoluteEncoder {
    
    private final AnalogInput analogInput;
    private final double maxVoltage;

    /**
     * @param analogInput The analog input port.
     * @param maxVoltage The maximum voltage returned by the encoder (usually 3.3V).
     */
    public AnalogAbsoluteEncoderWrapper(AnalogInput analogInput, double maxVoltage) {
        this.analogInput = analogInput;
        this.maxVoltage = maxVoltage;
    }

    private double offset = 0.0;

    @Override
    public void setOffset(double offsetRadians) {
        this.offset = offsetRadians;
    }

    @Override
    public void setDistancePerPulse(double distance) {}

    @Override
    public double getAbsolutePositionRad() {
        // Returns normalized absolute position from 0.0 to 1.0 scaled to rads
        double rads = (analogInput.getVoltage() / maxVoltage) * 2 * Math.PI;
        return (rads - offset) % (2 * Math.PI);
    }

    @Override
    public double getPosition() {
        return getAbsolutePositionRad();
    }

    @Override
    public double getVelocity() {
        return 0.0; // Pure analog absolute encoders generally don't measure velocity natively
    }
}

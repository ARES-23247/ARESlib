package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.AnalogInput;
import org.areslib.hardware.interfaces.AresAnalogSensor;

public class AnalogSensorWrapper implements AresAnalogSensor {
    private final AnalogInput analogInput;

    public AnalogSensorWrapper(AnalogInput analogInput) {
        this.analogInput = analogInput;
    }

    @Override
    public double getVoltage() {
        return analogInput.getVoltage();
    }
}

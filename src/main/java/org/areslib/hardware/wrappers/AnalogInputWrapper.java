package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.AnalogInput;
import org.areslib.hardware.sensors.AresAnalogSensor;

public class AnalogInputWrapper implements AresAnalogSensor {
    private final AnalogInput input;

    public AnalogInputWrapper(AnalogInput input) {
        this.input = input;
    }

    @Override
    public double getVoltage() {
        return input.getVoltage();
    }
}

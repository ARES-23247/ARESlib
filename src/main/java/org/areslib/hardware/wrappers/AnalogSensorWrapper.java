package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.AnalogInput;
import org.areslib.hardware.interfaces.AresAnalogSensor;

/**
 * AnalogSensorWrapper standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * AnalogSensorWrapper}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
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

package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.AnalogInput;
import org.areslib.hardware.interfaces.AresAnalogSensor;

/**
 * AnalogInputWrapper standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * AnalogInputWrapper}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
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

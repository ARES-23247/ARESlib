package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.DigitalChannel;
import org.areslib.hardware.interfaces.AresDigitalSensor;

/**
 * DigitalSensorWrapper standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * DigitalSensorWrapper}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public class DigitalSensorWrapper implements AresDigitalSensor {
  private final DigitalChannel digitalChannel;

  public DigitalSensorWrapper(DigitalChannel digitalChannel) {
    this.digitalChannel = digitalChannel;
    this.digitalChannel.setMode(DigitalChannel.Mode.INPUT);
  }

  @Override
  public boolean getState() {
    return digitalChannel.getState();
  }
}

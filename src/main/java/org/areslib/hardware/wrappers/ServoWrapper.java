package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.Servo;
import org.areslib.hardware.interfaces.AresServo;

/**
 * ServoWrapper standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * ServoWrapper}. Extracted and compiled as part of the ARESLib2 Code Audit for missing
 * documentation coverage.
 */
public class ServoWrapper implements AresServo {
  private final Servo servo;
  private double lastSentPosition = Double.NaN;
  private static final double CACHE_THRESHOLD = 0.005;

  public ServoWrapper(Servo servo) {
    this.servo = servo;
  }

  @Override
  public void setPosition(double position) {
    if (Double.isNaN(lastSentPosition) || Math.abs(position - lastSentPosition) > CACHE_THRESHOLD) {
      servo.setPosition(position);
      lastSentPosition = position;
    }
  }
}

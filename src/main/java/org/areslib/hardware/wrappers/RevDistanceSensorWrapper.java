package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.DistanceSensor;
import org.areslib.hardware.interfaces.AresDistanceSensor;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * RevDistanceSensorWrapper standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * RevDistanceSensorWrapper}. Extracted and compiled as part of the ARESLib2 Code Audit for missing
 * documentation coverage.
 */
public class RevDistanceSensorWrapper implements AresDistanceSensor {
  private final DistanceSensor distanceSensor;

  public RevDistanceSensorWrapper(DistanceSensor distanceSensor) {
    this.distanceSensor = distanceSensor;
  }

  @Override
  public double getDistanceMeters() {
    return distanceSensor.getDistance(DistanceUnit.METER);
  }
}

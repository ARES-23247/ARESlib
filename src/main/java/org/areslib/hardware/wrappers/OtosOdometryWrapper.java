package org.areslib.hardware.wrappers;

import com.qualcomm.hardware.sparkfun.SparkFunOTOS;
import org.areslib.hardware.faults.AresHardwareFaultInjector;
import org.areslib.hardware.faults.FaultMonitor;
import org.areslib.hardware.interfaces.OdometryIO;

/**
 * OtosOdometryWrapper standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * OtosOdometryWrapper}. Extracted and compiled as part of the ARESLib2 Code Audit for missing
 * documentation coverage.
 */
public class OtosOdometryWrapper implements OdometryIO, FaultMonitor {

  private final SparkFunOTOS otos;
  private boolean faultTripped = false;

  public OtosOdometryWrapper(SparkFunOTOS otos) {
    this.otos = otos;
  }

  @Override
  public void updateInputs(OdometryInputs inputs) {
    if (org.areslib.core.AresRobot.isSimulation()
        && AresHardwareFaultInjector.simulateEncoderShatter) {
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

    // ENFORCE WPILIB COORDINATE CONVENTIONS
    // FTC Standard: X = Right, Y = Forward
    // WPILib Standard: X = Forward, Y = Left
    inputs.xMeters = org.areslib.core.CoordinateUtil.inchesToMeters(otosPose.y);
    inputs.yMeters = -org.areslib.core.CoordinateUtil.inchesToMeters(otosPose.x);
    inputs.headingRadians = Math.toRadians(otosPose.h);

    inputs.xVelocityMetersPerSecond = org.areslib.core.CoordinateUtil.inchesToMeters(otosVel.y);
    inputs.yVelocityMetersPerSecond = -org.areslib.core.CoordinateUtil.inchesToMeters(otosVel.x);
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

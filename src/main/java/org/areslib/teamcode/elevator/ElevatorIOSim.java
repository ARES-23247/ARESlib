package org.areslib.teamcode.elevator;

/**
 * Simulated implementation of the ElevatorIO interface. Incorporates basic physical modeling for
 * testing without hardware.
 */
public class ElevatorIOSim implements ElevatorIO {

  // Sim State
  private double positionMeters = 0.0;
  private double velocityMps = 0.0;
  private double appliedVolts = 0.0;

  // Simple physical constants
  private static final double KV = 0.2; // roughly 0.2 meters/second per volt

  @Override
  public void updateInputs(ElevatorInputs inputs) {
    // Integrate Physics
    velocityMps = appliedVolts * KV;
    positionMeters += velocityMps * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

    // Apply constraints (elevator can't go below 0 or above max)
    if (positionMeters < 0.0) {
      positionMeters = 0.0;
      velocityMps = 0.0;
    }

    // Output to inputs struct
    inputs.positionMeters = this.positionMeters;
    inputs.velocityMps = this.velocityMps;
    inputs.appliedVolts = this.appliedVolts;
  }

  @Override
  public void setVoltage(double volts) {
    this.appliedVolts = Math.max(-12.0, Math.min(12.0, volts));
  }

  @Override
  public void setGrabberServo(double position) {
    // Servo moves instantly in sim
  }
}

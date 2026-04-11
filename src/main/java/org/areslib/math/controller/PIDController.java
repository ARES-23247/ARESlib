package org.areslib.math.controller;

/** Basic PID Controller. */
public class PIDController {
  private double kP, kI, kD;
  private double setpoint;
  private double prevError;
  private double integral;
  private boolean continuous;
  private double minimumInput, maximumInput;
  private double period;

  /**
   * Constructs a PIDController with the given constants. Default period is 0.02s (20ms).
   *
   * @param kP Proportional gain
   * @param kI Integral gain
   * @param kD Derivative gain
   */
  public PIDController(double kP, double kI, double kD) {
    this(kP, kI, kD, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
  }

  /**
   * Constructs a PIDController with the given constants and deterministic loop period.
   *
   * @param kP Proportional gain
   * @param kI Integral gain
   * @param kD Derivative gain
   * @param period The deterministic period in seconds between loop tracks
   */
  public PIDController(double kP, double kI, double kD, double period) {
    this.kP = kP;
    this.kI = kI;
    this.kD = kD;
    this.period = period;
  }

  /**
   * Enables continuous input, allowing the controller to wrap around (e.g., handles heading from
   * -180 to 180).
   *
   * @param minimumInput The minimum absolute value of the input.
   * @param maximumInput The maximum absolute value of the input.
   */
  public void enableContinuousInput(double minimumInput, double maximumInput) {
    this.continuous = true;
    this.minimumInput = minimumInput;
    this.maximumInput = maximumInput;
  }

  /**
   * Sets the setpoint for the PID controller.
   *
   * @param setpoint The desired setpoint.
   */
  public void setSetpoint(double setpoint) {
    this.setpoint = setpoint;
  }

  /**
   * Calculates the control output given a measured value and a setpoint. Uses the initialized
   * deterministic loop period.
   *
   * @param measurement The current measurement.
   * @param setpoint The desired setpoint.
   * @return The control output.
   */
  public double calculate(double measurement, double setpoint) {
    this.setpoint = setpoint;
    return calculate(measurement);
  }

  /**
   * Calculates the control output given a measured value based on the previously set setpoint. Uses
   * the initialized deterministic loop period.
   *
   * @param measurement The current measurement.
   * @return The control output.
   */
  public double calculate(double measurement) {
    double error = setpoint - measurement;

    if (continuous) {
      double range = maximumInput - minimumInput;
      // Safe modulus wrapping that handles negative values correctly
      error = ((error % range) + range) % range;
      if (error > range / 2.0) {
        error -= range;
      }
    }

    // Apply time-aware exact calculus implementations
    integral += error * period;

    double derivative = 0;
    if (period > 0) {
      derivative = (error - prevError) / period;
    }

    prevError = error;

    return kP * error + kI * integral + kD * derivative;
  }

  /**
   * Returns the current position error.
   *
   * @return The current error.
   */
  public double getPositionError() {
    return prevError;
  }

  /** Resets the previous error and integral term. */
  public void reset() {
    prevError = 0;
    integral = 0;
  }
}

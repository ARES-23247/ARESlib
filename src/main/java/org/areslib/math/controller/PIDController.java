package org.areslib.math.controller;

/** Basic PID Controller. */
public class PIDController {
  private double kP, kI, kD;
  private double setpoint;
  private double prevError;
  private double integral;
  private boolean continuous;
  private double minimumInput, maximumInput;
  private double period = 0; // 0 = dynamic
  private double lastTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();
  private double iZone = Double.POSITIVE_INFINITY;
  private double maxIntegral = Double.POSITIVE_INFINITY;
  private double minOutput = Double.NEGATIVE_INFINITY;
  private double maxOutput = Double.POSITIVE_INFINITY;

  /**
   * Constructs a PIDController with the given constants. Tracks time dynamically.
   *
   * @param kP Proportional gain
   * @param kI Integral gain
   * @param kD Derivative gain
   */
  public PIDController(double kP, double kI, double kD) {
    this.kP = kP;
    this.kI = kI;
    this.kD = kD;
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

    double currentTime = org.areslib.core.AresTimer.getFPGATimestamp();
    double dt = period > 0 ? period : (currentTime - lastTimeSeconds);
    lastTimeSeconds = currentTime;

    // Apply integral zone — zero the accumulator if error is too large
    if (Math.abs(error) > iZone) {
      integral = 0;
    } else {
      integral += error * dt;
      // Clamp integral to prevent unbounded windup during sustained error (e.g. stalls)
      integral = Math.max(-maxIntegral, Math.min(integral, maxIntegral));
    }

    double derivative = 0;
    if (dt > 0) {
      derivative = (error - prevError) / dt;
    }

    prevError = error;

    double output = kP * error + kI * integral + kD * derivative;
    return Math.max(minOutput, Math.min(output, maxOutput));
  }

  /**
   * Sets the integral zone. When the absolute error exceeds this threshold, the integral
   * accumulator is zeroed to prevent windup at hard stops or when far from the setpoint.
   *
   * @param iZone The integral zone threshold. Use {@code Double.POSITIVE_INFINITY} to disable.
   */
  public void setIntegralZone(double iZone) {
    this.iZone = iZone;
  }

  /**
   * Sets the output range to clamp the controller's output. Prevents runaway corrections.
   *
   * @param min The minimum output value.
   * @param max The maximum output value.
   */
  public void setOutputRange(double min, double max) {
    this.minOutput = min;
    this.maxOutput = max;
  }

  /**
   * Sets the maximum absolute value the integral accumulator can reach. Prevents unbounded windup
   * during sustained error (e.g., wheel stalls against field elements).
   *
   * @param maxIntegral The maximum integral magnitude. Use {@code Double.POSITIVE_INFINITY} to
   *     disable.
   */
  public void setMaxIntegral(double maxIntegral) {
    this.maxIntegral = maxIntegral;
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
    lastTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();
  }
}

package org.areslib.math.controller;

/**
 * Basic PID Controller.
 */
public class PIDController {
    private double kP, kI, kD;
    private double setpoint;
    private double prevError;
    private double integral;
    private boolean continuous;
    private double minimumInput, maximumInput;

    /**
     * Constructs a PIDController with the given constants.
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
     * Enables continuous input, allowing the controller to wrap around (e.g., handles heading from -180 to 180).
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
     * @param setpoint The desired setpoint.
     */
    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    /**
     * Calculates the control output given a measured value and a setpoint.
     * @param measurement The current measurement.
     * @param setpoint The desired setpoint.
     * @return The control output.
     */
    public double calculate(double measurement, double setpoint) {
        this.setpoint = setpoint;
        return calculate(measurement);
    }

    /**
     * Calculates the control output given a measured value based on the previously set setpoint.
     * @param measurement The current measurement.
     * @return The control output.
     */
    public double calculate(double measurement) {
        double error = setpoint - measurement;

        if (continuous) {
            double errorBound = (maximumInput - minimumInput) / 2.0;
            error = (error % (maximumInput - minimumInput) + (maximumInput - minimumInput)) % (maximumInput - minimumInput);
            if (error > errorBound) {
                error -= (maximumInput - minimumInput);
            }
        }

        integral += error;
        double derivative = error - prevError;
        prevError = error;

        return kP * error + kI * integral + kD * derivative;
    }

    /**
     * Resets the previous error and integral term.
     */
    public void reset() {
        prevError = 0;
        integral = 0;
    }
}

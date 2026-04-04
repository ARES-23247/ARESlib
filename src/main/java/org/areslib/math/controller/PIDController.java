package org.areslib.math.controller;

public class PIDController {
    private double kP, kI, kD;
    private double setpoint;
    private double prevError;
    private double integral;
    private boolean continuous;
    private double minimumInput, maximumInput;

    public PIDController(double kP, double kI, double kD) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
    }

    public void enableContinuousInput(double minimumInput, double maximumInput) {
        this.continuous = true;
        this.minimumInput = minimumInput;
        this.maximumInput = maximumInput;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    public double calculate(double measurement, double setpoint) {
        this.setpoint = setpoint;
        return calculate(measurement);
    }

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

    public void reset() {
        prevError = 0;
        integral = 0;
    }
}

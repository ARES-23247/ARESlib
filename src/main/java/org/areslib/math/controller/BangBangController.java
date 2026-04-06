package org.areslib.math.controller;

/**
 * Implements a bang-bang controller, which outputs either full power or no power
 * based on whether the measurement is below the setpoint.
 *
 * <p>Bang-bang control is extremely effective for controlling high-inertia mechanisms
 * like shooter flywheels where symmetrical motor action across the setpoint
 * is neither required nor desired (you don't want to actively brake a flywheel).
 */
public class BangBangController {
    private double m_tolerance;
    private double m_setpoint;

    /**
     * Creates a new bang-bang controller.
     *
     * <p>Always uses a default tolerance of 0.0.
     *
     * @param tolerance Tolerance for the controller.
     */
    public BangBangController(double tolerance) {
        m_tolerance = Math.max(tolerance, 0.0);
    }

    /**
     * Creates a new bang-bang controller with a tolerance of 0.0.
     */
    public BangBangController() {
        this(0.0);
    }

    /**
     * Sets the setpoint for the bang-bang controller.
     *
     * @param setpoint The desired setpoint.
     */
    public void setSetpoint(double setpoint) {
        m_setpoint = setpoint;
    }

    /**
     * Returns the current setpoint of the bang-bang controller.
     *
     * @return The current setpoint.
     */
    public double getSetpoint() {
        return m_setpoint;
    }

    /**
     * Sets the error within which the controller will stop applying power.
     *
     * @param tolerance The maximum acceptable error.
     */
    public void setTolerance(double tolerance) {
        m_tolerance = Math.max(tolerance, 0.0);
    }

    /**
     * Returns the current tolerance of the controller.
     *
     * @return The tolerance.
     */
    public double getTolerance() {
        return m_tolerance;
    }

    /**
     * Calculates the control output based on the current measurement.
     *
     * @param measurement The current measurement of the process variable.
     * @param setpoint    The desired setpoint.
     * @return 1.0 if the measurement is below the setpoint, otherwise 0.0.
     */
    public double calculate(double measurement, double setpoint) {
        m_setpoint = setpoint;
        return calculate(measurement);
    }

    /**
     * Calculates the control output based on the current measurement and the preconfigured setpoint.
     *
     * @param measurement The current measurement of the process variable.
     * @return 1.0 if the measurement is below the setpoint, otherwise 0.0.
     */
    public double calculate(double measurement) {
        if (measurement < (m_setpoint - m_tolerance)) {
            return 1.0;
        } else {
            return 0.0;
        }
    }

    /**
     * Determines if the error is within the tolerance.
     *
     * @param measurement The current measurement.
     * @return True if the error is within the tolerance.
     */
    public boolean atSetpoint(double measurement) {
        return Math.abs(m_setpoint - measurement) < m_tolerance;
    }
}

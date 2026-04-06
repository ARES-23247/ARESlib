package org.areslib.math.controller;

/**
 * A helper class that computes feedforward outputs for a simple elevator (vertical mechanism).
 */
public class ElevatorFeedforward {
    public final double ks;
    public final double kg;
    public final double kv;
    public final double ka;

    /**
     * Creates a new ElevatorFeedforward with the specified gains.
     *
     * @param ks The static gain.
     * @param kg The gravity gain.
     * @param kv The velocity gain.
     * @param ka The acceleration gain.
     */
    public ElevatorFeedforward(double ks, double kg, double kv, double ka) {
        this.ks = ks;
        this.kg = kg;
        this.kv = kv;
        this.ka = ka;
    }

    /**
     * Creates a new ElevatorFeedforward with the specified gains. Acceleration gain is defaulted to zero.
     *
     * @param ks The static gain.
     * @param kg The gravity gain.
     * @param kv The velocity gain.
     */
    public ElevatorFeedforward(double ks, double kg, double kv) {
        this(ks, kg, kv, 0);
    }

    /**
     * Calculates the feedforward from the gains and setpoints.
     *
     * @param velocity         The velocity setpoint.
     * @param acceleration     The acceleration setpoint.
     * @return The computed feedforward.
     */
    public double calculate(double velocity, double acceleration) {
        return ks * Math.signum(velocity) + kg + kv * velocity + ka * acceleration;
    }

    /**
     * Calculates the feedforward from the gains and velocity setpoint (acceleration is assumed to be zero).
     *
     * @param velocity The velocity setpoint.
     * @return The computed feedforward.
     */
    public double calculate(double velocity) {
        return calculate(velocity, 0);
    }
}

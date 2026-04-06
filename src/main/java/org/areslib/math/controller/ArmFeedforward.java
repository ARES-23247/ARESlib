package org.areslib.math.controller;

/**
 * A helper class that computes feedforward outputs for a simple arm (single-joint, rotating mechanism).
 */
public class ArmFeedforward {
    public final double ks;
    public final double kg;
    public final double kv;
    public final double ka;

    /**
     * Creates a new ArmFeedforward with the specified gains.
     *
     * @param ks The static gain.
     * @param kg The gravity gain.
     * @param kv The velocity gain.
     * @param ka The acceleration gain.
     */
    public ArmFeedforward(double ks, double kg, double kv, double ka) {
        this.ks = ks;
        this.kg = kg;
        this.kv = kv;
        this.ka = ka;
    }

    /**
     * Creates a new ArmFeedforward with the specified gains. Acceleration gain defaults to zero.
     *
     * @param ks The static gain.
     * @param kg The gravity gain.
     * @param kv The velocity gain.
     */
    public ArmFeedforward(double ks, double kg, double kv) {
        this(ks, kg, kv, 0);
    }

    /**
     * Calculates the feedforward from the gains and setpoints.
     *
     * @param positionRadians  The position angle setpoint in radians.
     * @param velocity         The velocity setpoint.
     * @param acceleration     The acceleration setpoint.
     * @return The computed feedforward.
     */
    public double calculate(double positionRadians, double velocity, double acceleration) {
        return ks * Math.signum(velocity) + kg * Math.cos(positionRadians) + kv * velocity + ka * acceleration;
    }

    /**
     * Calculates the feedforward from the gains and setpoints (assuming zero acceleration).
     *
     * @param positionRadians  The position angle setpoint in radians.
     * @param velocity         The velocity setpoint.
     * @return The computed feedforward.
     */
    public double calculate(double positionRadians, double velocity) {
        return calculate(positionRadians, velocity, 0);
    }
}

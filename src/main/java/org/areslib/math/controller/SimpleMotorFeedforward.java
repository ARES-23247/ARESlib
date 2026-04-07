package org.areslib.math.controller;

/**
 * A simple feedforward model for a motor, containing static, velocity, and acceleration terms.
 */
public class SimpleMotorFeedforward {
    private final double ks;
    private final double kv;
    private final double ka;

    /**
     * Constructs a SimpleMotorFeedforward.
     * @param ks The static friction gain.
     * @param kv The velocity gain.
     * @param ka The acceleration gain.
     */
    public SimpleMotorFeedforward(double ks, double kv, double ka) {
        this.ks = ks;
        this.kv = kv;
        this.ka = ka;
    }

    /**
     * Constructs a SimpleMotorFeedforward with no acceleration gain.
     * @param ks The static friction gain.
     * @param kv The velocity gain.
     */
    public SimpleMotorFeedforward(double ks, double kv) {
        this(ks, kv, 0.0);
    }

    /**
     * Calculates the feedforward given a specific velocity.
     * @param velocity The target velocity.
     * @return The calculated feedforward.
     */
    public double calculate(double velocity) {
        return ks * Math.signum(velocity) + kv * velocity;
    }

    /**
     * Calculates the feedforward given a current and next velocity over a specific timestep.
     * @param currentVelocity The current velocity.
     * @param nextVelocity The target next velocity.
     * @param dtSeconds The timestep in seconds.
     * @return The calculated feedforward.
     */
    public double calculate(double currentVelocity, double nextVelocity, double dtSeconds) {
        if (dtSeconds <= 0.0) {
            // No valid timestep — return steady-state feedforward without acceleration term.
            return ks * Math.signum(currentVelocity) + kv * currentVelocity;
        }
        double acceleration = (nextVelocity - currentVelocity) / dtSeconds;
        return ks * Math.signum(currentVelocity) + kv * currentVelocity + ka * acceleration;
    }
}

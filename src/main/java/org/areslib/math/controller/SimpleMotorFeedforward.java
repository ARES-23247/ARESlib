package org.areslib.math.controller;

public class SimpleMotorFeedforward {
    private final double ks;
    private final double kv;
    private final double ka;

    public SimpleMotorFeedforward(double ks, double kv, double ka) {
        this.ks = ks;
        this.kv = kv;
        this.ka = ka;
    }

    public SimpleMotorFeedforward(double ks, double kv) {
        this(ks, kv, 0.0);
    }

    public double calculate(double velocity) {
        return ks * Math.signum(velocity) + kv * velocity;
    }

    public double calculate(double currentVelocity, double nextVelocity, double dtSeconds) {
        double acceleration = (nextVelocity - currentVelocity) / dtSeconds;
        return ks * Math.signum(currentVelocity) + kv * currentVelocity + ka * acceleration;
    }
}

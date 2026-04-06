package org.areslib.math.filter;

/**
 * A class that limits the rate of change of an input value. Useful for implementing voltage,
 * setpoint, and/or output ramps. A slew-rate limit is most appropriate when the quantity being
 * controlled is a velocity or a voltage; if the quantity is a position, consider using a
 * TrapezoidProfile instead.
 */
public class SlewRateLimiter {
    private final double positiveRateLimit;
    private final double negativeRateLimit;
    private double prevVal;
    private double prevTime;

    /**
     * Creates a new SlewRateLimiter with the given positive and negative rate limits and initial
     * value.
     *
     * @param positiveRateLimit The rate-of-change limit in the positive direction, in units per second.
     * @param negativeRateLimit The rate-of-change limit in the negative direction, in units per second.
     * @param initialValue The initial value of the input.
     */
    public SlewRateLimiter(double positiveRateLimit, double negativeRateLimit, double initialValue) {
        this.positiveRateLimit = positiveRateLimit;
        this.negativeRateLimit = negativeRateLimit;
        this.prevVal = initialValue;
        this.prevTime = System.nanoTime() * 1e-9;
    }

    /**
     * Creates a new SlewRateLimiter with the given positive and negative rate limits and an initial
     * value of zero.
     *
     * @param positiveRateLimit The rate-of-change limit in the positive direction, in units per second.
     * @param negativeRateLimit The rate-of-change limit in the negative direction, in units per second.
     */
    public SlewRateLimiter(double positiveRateLimit, double negativeRateLimit) {
        this(positiveRateLimit, negativeRateLimit, 0.0);
    }

    /**
     * Creates a new SlewRateLimiter with the given rate limit and an initial value of zero.
     *
     * @param rateLimit The rate-of-change limit, in units per second.
     */
    public SlewRateLimiter(double rateLimit) {
        this(rateLimit, -rateLimit, 0.0);
    }

    /**
     * Filters the input to limit its slew rate.
     *
     * @param input The input value whose slew rate is to be limited.
     * @return The filtered value, which will not change faster than the slew rate.
     */
    public double calculate(double input) {
        double currentTime = System.nanoTime() * 1e-9;
        double elapsedTime = currentTime - prevTime;
        double delta = input - prevVal;
        
        if (delta > 0) {
            prevVal += Math.min(delta, positiveRateLimit * elapsedTime);
        } else {
            prevVal += Math.max(delta, negativeRateLimit * elapsedTime);
        }
        
        prevTime = currentTime;
        return prevVal;
    }

    /**
     * Resets the slew rate limiter to the specified value; ignores the rate limit when doing so.
     *
     * @param value The value to reset to.
     */
    public void reset(double value) {
        prevVal = value;
        prevTime = System.nanoTime() * 1e-9;
    }
}

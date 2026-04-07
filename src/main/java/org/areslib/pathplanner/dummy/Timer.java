package org.areslib.pathplanner.dummy;

/**
 * A dummy implementation of the WPILib Timer class to ensure compatibility with PathPlanner
 * without requiring the full WPILib HAL or Android runtime.
 * 
 * This class tracks time natively using `System.nanoTime()` allowing accurate timings during
 * headless desktop simulation and tests.
 */
public class Timer {
    private long startTimeNanos;
    private long accumulatedNanos;
    private boolean running;

    /**
     * Get the FPGA timestamp in seconds.
     * In this dummy implementation, this is mathematically backed by System.nanoTime().
     *
     * @return the current time in seconds
     */
    public static double getFPGATimestamp() { return System.nanoTime() / 1e9; }

    /**
     * Reset the timer to zero.
     */
    public void reset() {
        accumulatedNanos = 0;
        startTimeNanos = System.nanoTime();
    }

    /**
     * Start the timer tracking.
     */
    public void start() {
        if (!running) {
            startTimeNanos = System.nanoTime();
            running = true;
        }
    }

    /**
     * Stop the timer tracking.
     */
    public void stop() {
        if (running) {
            accumulatedNanos += System.nanoTime() - startTimeNanos;
            running = false;
        }
    }

    /**
     * Restart the timer by resetting it and then starting it.
     */
    public void restart() {
        reset();
        start();
    }

    /**
     * Retrieve the current accumulated time in seconds.
     *
     * @return the elapsed time in seconds
     */
    public double get() {
        if (running) {
            return (accumulatedNanos + (System.nanoTime() - startTimeNanos)) / 1e9;
        }
        return accumulatedNanos / 1e9;
    }

    /**
     * Check if a specific amount of time has elapsed.
     *
     * @param seconds time to check against the elapsed time
     * @return true if the elapsed time is greater than or equal to the provided amount
     */
    public boolean hasElapsed(double seconds) {
        return get() >= seconds;
    }
}
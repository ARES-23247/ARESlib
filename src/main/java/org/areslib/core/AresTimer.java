package org.areslib.core;

/**
 * Universal Timing engine for the ARESLib framework.
 *
 * <p>Tracks absolute uptime and exact delta-time (`dt`) loops leveraging native system nanosecond
 * clocks. Essential for mathematically rigorous integral loops across FTC hardware without
 * resorting to unconstrained ping-pong control loop delays.
 */
public class AresTimer {
  private static long startTimeNanos = System.nanoTime();
  private static long lastTimeNanos = startTimeNanos;

  /**
   * Resets the entire timer subsystem state to zero. Only used heavily in isolated test
   * configurations.
   */
  public static void reset() {
    startTimeNanos = System.nanoTime();
    lastTimeNanos = startTimeNanos;
  }

  /**
   * Identical to WPILib's global FPGATimestamp. Retains time since robot initialization.
   *
   * @return Elapsed time since framework boot up in seconds.
   */
  public static double getFPGATimestamp() {
    return (System.nanoTime() - startTimeNanos) / 1e9;
  }

  /**
   * Computes the exact delta-time (dt) since the last time this function was called. Use this
   * selectively per-subsystem, or track locally in the subsystem cache.
   *
   * @return The exact delta-time in seconds since this was last polled.
   */
  public static double measureDeltaTimeSecs() {
    long currentNanos = System.nanoTime();
    double dt = (currentNanos - lastTimeNanos) / 1e9;
    lastTimeNanos = currentNanos;
    return dt;
  }
}

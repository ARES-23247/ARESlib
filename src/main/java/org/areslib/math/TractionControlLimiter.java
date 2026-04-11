package org.areslib.math;

/**
 * Advanced 2D Slew Rate Limiter to prevent carpet/tile slippage and odometry drift.
 *
 * <p>Standard SlewRateLimiters operate independently on X and Y axes. If both max out, the robot
 * accelerates diagonally at 1.41x the limit, instantly breaking surface friction. This class
 * ensures the total 2D acceleration vector never exceeds the maximum static friction coefficient.
 *
 * <p>For FTC, the typical maximum acceleration before wheel slip on foam tiles is approximately
 * 8-10 m/s^2 depending on wheel material and robot weight.
 *
 * <p>Students: Wrap your ChassisSpeeds X/Y inputs with this to prevent the robot from sliding
 * sideways during aggressive teleop maneuvers.
 */
public class TractionControlLimiter {
  private final double maxAccelMetersPerSecSq;
  private double lastVx = 0.0;
  private double lastVy = 0.0;
  private double lastTimeSeconds;

  /**
   * Initializes the 2D Traction Control limiter.
   *
   * @param maxAccelMetersPerSecSq Maximum physical acceleration before tire slip occurs. For FTC
   *     mecanum on foam tiles, typical values are 8.0 to 10.0 m/s^2.
   */
  public TractionControlLimiter(double maxAccelMetersPerSecSq) {
    this.maxAccelMetersPerSecSq = maxAccelMetersPerSecSq;
    this.lastTimeSeconds = System.nanoTime() / 1_000_000_000.0;
  }

  /**
   * Calculates the max achievable velocity vector without slipping tires.
   *
   * @param targetVx Target X velocity (m/s).
   * @param targetVy Target Y velocity (m/s).
   * @return A double array [safeVx, safeVy] to apply to ChassisSpeeds.
   */
  public double[] calculate(double targetVx, double targetVy) {
    double currentTime = System.nanoTime() / 1_000_000_000.0;
    double dt = currentTime - lastTimeSeconds;
    lastTimeSeconds = currentTime;

    // Prevent divide-by-zero on first loop
    if (dt <= 0.0) return new double[] {lastVx, lastVy};

    // Calculate requested velocity change
    double deltaVx = targetVx - lastVx;
    double deltaVy = targetVy - lastVy;
    double deltaNorm = Math.sqrt(deltaVx * deltaVx + deltaVy * deltaVy);

    // Total acceleration required to achieve this change
    double currentAccel = deltaNorm / dt;

    if (currentAccel > maxAccelMetersPerSecSq) {
      // Scale down the change to exactly match peak acceleration
      double scalar = maxAccelMetersPerSecSq / currentAccel;
      deltaVx *= scalar;
      deltaVy *= scalar;
    }

    lastVx += deltaVx;
    lastVy += deltaVy;

    // Ensure we don't float slightly off 0.0 due to dt calculation margins
    double targetNorm = Math.sqrt(targetVx * targetVx + targetVy * targetVy);
    double lastNorm = Math.sqrt(lastVx * lastVx + lastVy * lastVy);
    if (targetNorm == 0.0 && lastNorm < 0.05) {
      lastVx = 0.0;
      lastVy = 0.0;
    }

    return new double[] {lastVx, lastVy};
  }

  /** Resets the limiter state. Call when the robot transitions to a new operational mode. */
  public void reset() {
    lastVx = 0.0;
    lastVy = 0.0;
    lastTimeSeconds = System.nanoTime() / 1_000_000_000.0;
  }
}

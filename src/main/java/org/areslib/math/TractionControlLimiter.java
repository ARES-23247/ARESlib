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
  private final double m_maxAccelMetersPerSecSq;
  private double m_lastVx = 0.0;
  private double m_lastVy = 0.0;
  private double m_lastTimeSeconds;

  /**
   * Initializes the 2D Traction Control limiter.
   *
   * @param maxAccelMetersPerSecSq Maximum physical acceleration before tire slip occurs. For FTC
   *     mecanum on foam tiles, typical values are 8.0 to 10.0 m/s^2.
   */
  public TractionControlLimiter(double maxAccelMetersPerSecSq) {
    this.m_maxAccelMetersPerSecSq = maxAccelMetersPerSecSq;
    this.m_lastTimeSeconds = System.nanoTime() / 1_000_000_000.0;
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
    double dt = currentTime - m_lastTimeSeconds;
    m_lastTimeSeconds = currentTime;

    // Prevent divide-by-zero on first loop
    if (dt <= 0.0) return new double[] {m_lastVx, m_lastVy};

    // Calculate requested velocity change
    double deltaVx = targetVx - m_lastVx;
    double deltaVy = targetVy - m_lastVy;
    double deltaNorm = Math.sqrt(deltaVx * deltaVx + deltaVy * deltaVy);

    // Total acceleration required to achieve this change
    double currentAccel = deltaNorm / dt;

    if (currentAccel > m_maxAccelMetersPerSecSq) {
      // Scale down the change to exactly match peak acceleration
      double scalar = m_maxAccelMetersPerSecSq / currentAccel;
      deltaVx *= scalar;
      deltaVy *= scalar;
    }

    m_lastVx += deltaVx;
    m_lastVy += deltaVy;

    // Ensure we don't float slightly off 0.0 due to dt calculation margins
    double targetNorm = Math.sqrt(targetVx * targetVx + targetVy * targetVy);
    double lastNorm = Math.sqrt(m_lastVx * m_lastVx + m_lastVy * m_lastVy);
    if (targetNorm == 0.0 && lastNorm < 0.05) {
      m_lastVx = 0.0;
      m_lastVy = 0.0;
    }

    return new double[] {m_lastVx, m_lastVy};
  }

  /** Resets the limiter state. Call when the robot transitions to a new operational mode. */
  public void reset() {
    m_lastVx = 0.0;
    m_lastVy = 0.0;
    m_lastTimeSeconds = System.nanoTime() / 1_000_000_000.0;
  }
}

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
  private double lastTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();

  /** Pre-allocated result buffer for {@link #calculate}. */
  private final double[] resultCache = new double[2];

  /**
   * Initializes the 2D Traction Control limiter.
   *
   * @param maxAccelMetersPerSecSq Maximum physical acceleration before tire slip occurs. For FTC
   *     mecanum on foam tiles, typical values are 8.0 to 10.0 m/s^2.
   */
  public TractionControlLimiter(double maxAccelMetersPerSecSq) {
    this.maxAccelMetersPerSecSq = maxAccelMetersPerSecSq;
  }

  /**
   * Calculates the max achievable velocity vector without slipping tires.
   *
   * @param targetVx Target X velocity (m/s).
   * @param targetVy Target Y velocity (m/s).
   * @return A double array [safeVx, safeVy] to apply to ChassisSpeeds.
   */
  public double[] calculate(double targetVx, double targetVy) {
    return calculate(targetVx, targetVy, 0.0, 0.0);
  }

  /**
   * Heading-aware variant using dynamic delta-time natively.
   *
   * @param targetVx Target X velocity (m/s).
   * @param targetVy Target Y velocity (m/s).
   * @param omegaRadPerSec Current rotational velocity (rad/s).
   * @param wheelbaseRadiusMeters Distance from robot center to the farthest wheel (meters).
   * @return A double array [safeVx, safeVy] to apply to ChassisSpeeds.
   */
  public double[] calculate(
      double targetVx, double targetVy, double omegaRadPerSec, double wheelbaseRadiusMeters) {
    double currentTime = org.areslib.core.AresTimer.getFPGATimestamp();
    double dt = currentTime - lastTimeSeconds;
    lastTimeSeconds = currentTime;

    return calculate(targetVx, targetVy, omegaRadPerSec, wheelbaseRadiusMeters, dt);
  }

  /**
   * Heading-aware variant that accounts for rotational velocity stealing from the translational
   * acceleration budget. The effective wheel-contact acceleration increases with omega, so we
   * reduce the available linear acceleration proportionally.
   *
   * <p>The formula is: availableAccel = maxAccel * max(0, 1 - |omega * wheelbaseRadius| / maxAccel)
   *
   * @param targetVx Target X velocity (m/s).
   * @param targetVy Target Y velocity (m/s).
   * @param omegaRadPerSec Current rotational velocity (rad/s).
   * @param wheelbaseRadiusMeters Distance from robot center to the farthest wheel (meters).
   * @return A double array [safeVx, safeVy] to apply to ChassisSpeeds.
   */
  public double[] calculate(
      double targetVx,
      double targetVy,
      double omegaRadPerSec,
      double wheelbaseRadiusMeters,
      double dtSeconds) {

    // Prevent divide-by-zero on first loop
    if (dtSeconds <= 0.0) {
      resultCache[0] = lastVx;
      resultCache[1] = lastVy;
      return resultCache;
    }

    // Calculate the tangential velocity at the farthest wheel due to rotation (m/s).
    // This velocity steals from the friction budget available for linear acceleration.
    double rotationalTangentialVelocity = Math.abs(omegaRadPerSec) * wheelbaseRadiusMeters;
    double effectiveMaxAccel =
        maxAccelMetersPerSecSq
            * Math.max(0.0, 1.0 - rotationalTangentialVelocity / maxAccelMetersPerSecSq);

    // Calculate requested velocity change
    double deltaVx = targetVx - lastVx;
    double deltaVy = targetVy - lastVy;
    double deltaNorm = Math.sqrt(deltaVx * deltaVx + deltaVy * deltaVy);

    // Total acceleration required to achieve this change
    double currentAccel = deltaNorm / dtSeconds;

    if (currentAccel > effectiveMaxAccel && effectiveMaxAccel > 0.0) {
      // Scale down the change to exactly match peak acceleration
      double scalar = effectiveMaxAccel / currentAccel;
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

    resultCache[0] = lastVx;
    resultCache[1] = lastVy;
    return resultCache;
  }

  /** Resets the limiter state. Call when the robot transitions to a new operational mode. */
  public void reset() {
    lastVx = 0.0;
    lastVy = 0.0;
    lastTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();
  }
}

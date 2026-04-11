package org.areslib.math.filter;

/**
 * A lightweight, dependency-free 1-Dimensional Kalman Filter for state tracking.
 *
 * <p>Optimally tracks a 1D state consisting of [Position, Velocity] by merging a mathematical
 * predictive model with noisy sensor measurements (e.g., encoders). This is particularly effective
 * for flywheels and elevators where low-pass filters introduce unacceptable phase lag.
 *
 * <p>This implementation uses explicit algebraic expansions of the standard 2x2 Kalman equations,
 * completely avoiding matrix object allocations to ensure 0 GC overhead during loops.
 */
public class KalmanFilter1D {

  // State vector
  private double pos = 0.0;
  private double vel = 0.0;

  // State covariance matrix (2x2)
  private double p00 = 1.0;
  private double p01 = 0.0;
  private double p10 = 0.0;
  private double p11 = 1.0;

  // Process noise covariance (Q)
  private double qPos;
  private double qVel;

  /**
   * Constructs a 1D Kalman Filter.
   *
   * @param qPos Expected variance in the position model (e.g., 0.001).
   * @param qVel Expected variance in the velocity model (e.g., 0.1).
   */
  public KalmanFilter1D(double qPos, double qVel) {
    this.qPos = qPos;
    this.qVel = qVel;
  }

  /**
   * Resets the filter to a known state.
   *
   * @param initialPos Initial position.
   * @param initialVel Initial velocity.
   */
  public void reset(double initialPos, double initialVel) {
    pos = initialPos;
    vel = initialVel;
    p00 = 1.0;
    p01 = 0.0;
    p10 = 0.0;
    p11 = 1.0;
  }

  /**
   * Predicts the next state using the kinematic model. Must be called once per loop before
   * updating.
   *
   * @param dtSeconds The time elapsed since the last prediction.
   */
  public void predict(double dtSeconds) {
    // x_k|k-1 = A * x_k-1|k-1
    pos += dtSeconds * vel;
    // vel remains unchanged (constant velocity model)

    // P_k|k-1 = A * P_k-1|k-1 * A^T + Q
    // A = [1, dt]
    //     [0,  1]
    double p00New = p00 + dtSeconds * (p10 + p01) + dtSeconds * dtSeconds * p11 + qPos;
    double p01New = p01 + dtSeconds * p11;
    double p10New = p10 + dtSeconds * p11;
    double p11New = p11 + qVel;

    p00 = p00New;
    p01 = p01New;
    p10 = p10New;
    p11 = p11New;
  }

  /**
   * Updates the state with a new position measurement.
   *
   * @param measurement The noisy position reading.
   * @param rVariance The variance (noise) of the measurement.
   */
  public void updatePosition(double measurement, double rVariance) {
    // H = [1, 0]
    // y = z - H * x
    double y = measurement - pos;

    // S = H * P * H^T + R
    double s = p00 + rVariance;

    // K = P * H^T * S^-1
    double k0 = p00 / s;
    double k1 = p10 / s;

    // x_k|k = x_k|k-1 + K * y
    pos += k0 * y;
    vel += k1 * y;

    // P_k|k = (I - K * H) * P_k|k-1
    double p00New = (1 - k0) * p00;
    double p01New = (1 - k0) * p01;
    double p10New = -k1 * p00 + p10;
    double p11New = -k1 * p01 + p11;

    p00 = p00New;
    p01 = p01New;
    p10 = p10New;
    p11 = p11New;
  }

  /**
   * Updates the state with a new velocity measurement.
   *
   * @param measurement The noisy velocity reading.
   * @param rVariance The variance (noise) of the measurement.
   */
  public void updateVelocity(double measurement, double rVariance) {
    // H = [0, 1]
    // y = z - H * x
    double y = measurement - vel;

    // S = H * P * H^T + R
    double s = p11 + rVariance;

    // K = P * H^T * S^-1
    double k0 = p01 / s;
    double k1 = p11 / s;

    // x_k|k = x_k|k-1 + K * y
    pos += k0 * y;
    vel += k1 * y;

    // P_k|k = (I - K * H) * P_k|k-1
    double p00New = p00 - k0 * p10;
    double p01New = p01 - k0 * p11;
    double p10New = (1 - k1) * p10;
    double p11New = (1 - k1) * p11;

    p00 = p00New;
    p01 = p01New;
    p10 = p10New;
    p11 = p11New;
  }

  /**
   * Returns the optimally filtered position.
   *
   * @return The optimally filtered position.
   */
  public double getPosition() {
    return pos;
  }

  /**
   * Returns the optimally filtered velocity.
   *
   * @return The optimally filtered velocity.
   */
  public double getVelocity() {
    return vel;
  }
}

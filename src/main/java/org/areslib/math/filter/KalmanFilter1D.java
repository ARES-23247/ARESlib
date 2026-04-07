package org.areslib.math.filter;

/**
 * A lightweight, dependency-free 1-Dimensional Kalman Filter for state tracking.
 * <p>
 * Optimally tracks a 1D state consisting of [Position, Velocity] by merging a mathematical
 * predictive model with noisy sensor measurements (e.g., encoders). 
 * This is particularly effective for flywheels and elevators where low-pass filters
 * introduce unacceptable phase lag.
 * <p>
 * This implementation uses explicit algebraic expansions of the standard 2x2 Kalman
 * equations, completely avoiding matrix object allocations to ensure 0 GC overhead during loops.
 */
public class KalmanFilter1D {

    // State vector
    private double m_pos = 0.0;
    private double m_vel = 0.0;

    // State covariance matrix (2x2)
    private double m_p00 = 1.0;
    private double m_p01 = 0.0;
    private double m_p10 = 0.0;
    private double m_p11 = 1.0;

    // Process noise covariance (Q)
    private double m_qPos;
    private double m_qVel;

    /**
     * Constructs a 1D Kalman Filter.
     *
     * @param qPos Expected variance in the position model (e.g., 0.001).
     * @param qVel Expected variance in the velocity model (e.g., 0.1).
     */
    public KalmanFilter1D(double qPos, double qVel) {
        m_qPos = qPos;
        m_qVel = qVel;
    }

    /**
     * Resets the filter to a known state.
     *
     * @param initialPos Initial position.
     * @param initialVel Initial velocity.
     */
    public void reset(double initialPos, double initialVel) {
        m_pos = initialPos;
        m_vel = initialVel;
        m_p00 = 1.0;
        m_p01 = 0.0;
        m_p10 = 0.0;
        m_p11 = 1.0;
    }

    /**
     * Predicts the next state using the kinematic model.
     * Must be called once per loop before updating.
     *
     * @param dtSeconds The time elapsed since the last prediction.
     */
    public void predict(double dtSeconds) {
        // x_k|k-1 = A * x_k-1|k-1
        m_pos += dtSeconds * m_vel;
        // m_vel remains unchanged (constant velocity model)

        // P_k|k-1 = A * P_k-1|k-1 * A^T + Q
        // A = [1, dt]
        //     [0,  1]
        double p00_new = m_p00 + dtSeconds * (m_p10 + m_p01) + dtSeconds * dtSeconds * m_p11 + m_qPos;
        double p01_new = m_p01 + dtSeconds * m_p11;
        double p10_new = m_p10 + dtSeconds * m_p11;
        double p11_new = m_p11 + m_qVel;

        m_p00 = p00_new;
        m_p01 = p01_new;
        m_p10 = p10_new;
        m_p11 = p11_new;
    }

    /**
     * Updates the state with a new position measurement.
     *
     * @param measurement The noisy position reading.
     * @param rVariance   The variance (noise) of the measurement.
     */
    public void updatePosition(double measurement, double rVariance) {
        // H = [1, 0]
        // y = z - H * x
        double y = measurement - m_pos;

        // S = H * P * H^T + R
        double s = m_p00 + rVariance;

        // K = P * H^T * S^-1
        double k0 = m_p00 / s;
        double k1 = m_p10 / s;

        // x_k|k = x_k|k-1 + K * y
        m_pos += k0 * y;
        m_vel += k1 * y;

        // P_k|k = (I - K * H) * P_k|k-1
        double p00_new = (1 - k0) * m_p00;
        double p01_new = (1 - k0) * m_p01;
        double p10_new = -k1 * m_p00 + m_p10;
        double p11_new = -k1 * m_p01 + m_p11;

        m_p00 = p00_new;
        m_p01 = p01_new;
        m_p10 = p10_new;
        m_p11 = p11_new;
    }
    
    /**
     * Updates the state with a new velocity measurement.
     *
     * @param measurement The noisy velocity reading.
     * @param rVariance   The variance (noise) of the measurement.
     */
    public void updateVelocity(double measurement, double rVariance) {
        // H = [0, 1]
        // y = z - H * x
        double y = measurement - m_vel;

        // S = H * P * H^T + R
        double s = m_p11 + rVariance;

        // K = P * H^T * S^-1
        double k0 = m_p01 / s;
        double k1 = m_p11 / s;

        // x_k|k = x_k|k-1 + K * y
        m_pos += k0 * y;
        m_vel += k1 * y;

        // P_k|k = (I - K * H) * P_k|k-1
        double p00_new = m_p00 - k0 * m_p10;
        double p01_new = m_p01 - k0 * m_p11;
        double p10_new = (1 - k1) * m_p10;
        double p11_new = (1 - k1) * m_p11;

        m_p00 = p00_new;
        m_p01 = p01_new;
        m_p10 = p10_new;
        m_p11 = p11_new;
    }

    /**
     * Returns the optimally filtered position.
     *
     * @return The optimally filtered position.
     */
    public double getPosition() {
        return m_pos;
    }

    /**
     * Returns the optimally filtered velocity.
     *
     * @return The optimally filtered velocity.
     */
    public double getVelocity() {
        return m_vel;
    }
}

package org.areslib.math.controller;

/**
 * A PID Controller configured to follow a Trapezoidal Motion Profile.
 * Generates smooth deceleration and velocity limiting instead of aggressive snaps.
 */
public class ProfiledPIDController {
    private final PIDController m_controller;
    private double m_maxVelocity;
    private double m_maxAcceleration;
    private double m_period;

    private double m_goalPosition;
    private double m_setpointPosition;
    private double m_setpointVelocity;

    /**
     * @param kp The proportional coefficient.
     * @param ki The integral coefficient.
     * @param kd The derivative coefficient.
     * @param maxVelocity The maximum allowable velocity.
     * @param maxAcceleration The maximum allowable acceleration.
     */
    public ProfiledPIDController(
            double kp, double ki, double kd, double maxVelocity, double maxAcceleration) {
        this(kp, ki, kd, maxVelocity, maxAcceleration, org.areslib.core.AresRobot.LOOP_PERIOD_SECS);
    }
    
    /**
     * @param kp The proportional coefficient.
     * @param ki The integral coefficient.
     * @param kd The derivative coefficient.
     * @param maxVelocity The maximum allowable velocity.
     * @param maxAcceleration The maximum allowable acceleration.
     * @param period The exact control loop period in seconds (vital for determinism).
     */
    public ProfiledPIDController(double kp, double ki, double kd, double maxVelocity, double maxAcceleration, double period) {
        m_controller = new PIDController(kp, ki, kd, period);
        m_maxVelocity = maxVelocity;
        m_maxAcceleration = maxAcceleration;
        m_period = period;
    }

    /**
     * Sets the final mathematical goal the system should reach.
     * @param goal The target position.
     */
    public void setGoal(double goal) {
        m_goalPosition = goal;
    }

    /**
     * Sets the current state of the mechanism. Call this once when initializing a movement.
     * @param currentPosition The current position of the mechanism.
     * @param currentVelocity The current velocity of the mechanism.
     */
    public void reset(double currentPosition, double currentVelocity) {
        m_setpointPosition = currentPosition;
        m_setpointVelocity = currentVelocity;
        m_controller.reset();
    }

    /**
     * Calculates the next output of the controller using deterministic simulation time.
     * @param currentMeasurement The current position measurement.
     * @return The control effort to apply.
     */
    public double calculate(double currentMeasurement) {
        double dt = m_period;

        if (dt <= 0.0) return 0.0; // Prevent divide by zero

        // 1. Calculate the distance left to the goal
        double distanceToGo = m_goalPosition - m_setpointPosition;
        
        // 2. What velocity do we need to stop perfectly at the goal?
        // v^2 = 2 * a * d => v = sqrt(2 * a * d)
        double maxReachableVelocity = Math.sqrt(2 * m_maxAcceleration * Math.abs(distanceToGo));
        
        // 3. Constrain desired velocity to our max velocity profile limit
        double targetVelocity = Math.min(maxReachableVelocity, m_maxVelocity);
        
        // Preserve Direction
        targetVelocity *= Math.signum(distanceToGo);

        // 4. Accelerate towards the target velocity using max acceleration
        double velocityError = targetVelocity - m_setpointVelocity;
        double maxVelocityStep = m_maxAcceleration * dt;
        double newVelocity = m_setpointVelocity + Math.max(-maxVelocityStep, Math.min(maxVelocityStep, velocityError));

        // 5. Integrate to find the current active setpoint
        m_setpointPosition += newVelocity * dt;
        m_setpointVelocity = newVelocity;

        // 6. The standard PID calculates against our *profiled* active setpoint, not the absolute final goal.
        return m_controller.calculate(currentMeasurement, m_setpointPosition);
    }

    public double getSetpointPosition() {
        return m_setpointPosition;
    }

    public double getSetpointVelocity() {
        return m_setpointVelocity;
    }
}

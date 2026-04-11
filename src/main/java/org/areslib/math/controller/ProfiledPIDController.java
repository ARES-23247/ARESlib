package org.areslib.math.controller;

/**
 * A PID Controller configured to follow a Trapezoidal Motion Profile. Generates smooth deceleration
 * and velocity limiting instead of aggressive snaps.
 */
public class ProfiledPIDController {
  private final PIDController controller;
  private double maxVelocity;
  private double maxAcceleration;
  private double period;

  private double goalPosition;
  private double setpointPosition;
  private double setpointVelocity;

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
  public ProfiledPIDController(
      double kp, double ki, double kd, double maxVelocity, double maxAcceleration, double period) {
    controller = new PIDController(kp, ki, kd, period);
    this.maxVelocity = maxVelocity;
    this.maxAcceleration = maxAcceleration;
    this.period = period;
  }

  /**
   * Sets the final mathematical goal the system should reach.
   *
   * @param goal The target position.
   */
  public void setGoal(double goal) {
    goalPosition = goal;
  }

  /**
   * Returns the current goal position.
   *
   * @return The goal position.
   */
  public double getGoal() {
    return goalPosition;
  }

  /**
   * Sets the velocity and acceleration constraints for the profile
   *
   * @param maxVelocity Maximum velocity
   * @param maxAcceleration Maximum acceleration
   */
  public void setConstraints(double maxVelocity, double maxAcceleration) {
    this.maxVelocity = maxVelocity;
    this.maxAcceleration = maxAcceleration;
  }

  /**
   * Enables continuous input wrapping on the internal PID controller. This is essential for angular
   * control (e.g., heading) where -PI and PI are adjacent.
   *
   * @param minimumInput The minimum input value (e.g., -Math.PI).
   * @param maximumInput The maximum input value (e.g., Math.PI).
   */
  public void enableContinuousInput(double minimumInput, double maximumInput) {
    controller.enableContinuousInput(minimumInput, maximumInput);
  }

  /**
   * Sets the current state of the mechanism. Call this once when initializing a movement.
   *
   * @param currentPosition The current position of the mechanism.
   * @param currentVelocity The current velocity of the mechanism.
   */
  public void reset(double currentPosition, double currentVelocity) {
    setpointPosition = currentPosition;
    setpointVelocity = currentVelocity;
    controller.reset();
  }

  /**
   * Calculates the next output of the controller using deterministic simulation time.
   *
   * @param currentMeasurement The current position measurement.
   * @return The control effort to apply.
   */
  public double calculate(double currentMeasurement) {
    double dt = period;

    if (dt <= 0.0) return 0.0; // Prevent divide by zero

    // 1. Calculate the distance left to the goal
    double distanceToGo = goalPosition - setpointPosition;

    // 2. What velocity do we need to stop perfectly at the goal?
    // v^2 = 2 * a * d => v = sqrt(2 * a * d)
    double maxReachableVelocity = Math.sqrt(2 * maxAcceleration * Math.abs(distanceToGo));

    // 3. Constrain desired velocity to our max velocity profile limit
    double targetVelocity = Math.min(maxReachableVelocity, maxVelocity);

    // Preserve Direction
    targetVelocity *= Math.signum(distanceToGo);

    // 4. Accelerate towards the target velocity using max acceleration
    double velocityError = targetVelocity - setpointVelocity;
    double maxVelocityStep = maxAcceleration * dt;
    double newVelocity =
        setpointVelocity + Math.max(-maxVelocityStep, Math.min(maxVelocityStep, velocityError));

    // 5. Integrate to find the current active setpoint
    setpointPosition += newVelocity * dt;
    setpointVelocity = newVelocity;

    // 6. The standard PID calculates against our *profiled* active setpoint, not the absolute final
    // goal.
    return controller.calculate(currentMeasurement, setpointPosition);
  }

  public double getSetpointPosition() {
    return setpointPosition;
  }

  public double getSetpointVelocity() {
    return setpointVelocity;
  }
}

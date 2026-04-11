package org.areslib.math.controller;

/**
 * A standalone trapezoidal motion profile generator, decoupled from any PID controller.
 *
 * <p>Generates smooth motion with configurable maximum velocity and acceleration constraints. The
 * profile produces position and velocity setpoints at each timestep that respect kinematic limits,
 * allowing mechanisms to accelerate, cruise, and decelerate smoothly.
 *
 * <p><b>Usage:</b> Call {@link #calculate(double)} each loop iteration with a fixed dt to advance
 * the profile. Use the returned {@link State} to feed a PID controller, feedforward, or directly
 * drive open-loop mechanisms.
 *
 * <pre>{@code
 * TrapezoidProfile profile = new TrapezoidProfile(
 *     new TrapezoidProfile.Constraints(maxVel, maxAccel),
 *     new TrapezoidProfile.State(targetPos, 0),  // goal
 *     new TrapezoidProfile.State(currentPos, 0)   // initial
 * );
 *
 * // In periodic():
 * State setpoint = profile.calculate(0.02);  // 20ms dt
 * motor.setPower(pid.calculate(encoder.getPosition(), setpoint.position));
 * }</pre>
 */
public class TrapezoidProfile {

  /** Kinematic constraints for the profile. */
  public static class Constraints {
    /** Maximum velocity in units per second. */
    public final double maxVelocity;

    /** Maximum acceleration in units per second². */
    public final double maxAcceleration;

    /**
     * @param maxVelocity Maximum velocity magnitude.
     * @param maxAcceleration Maximum acceleration magnitude.
     */
    public Constraints(double maxVelocity, double maxAcceleration) {
      this.maxVelocity = Math.abs(maxVelocity);
      this.maxAcceleration = Math.abs(maxAcceleration);
    }
  }

  /** A state along the profile (position + velocity at a point in time). */
  public static class State {
    /** Position in arbitrary units. */
    public double position;

    /** Velocity in units per second. */
    public double velocity;

    public State(double position, double velocity) {
      this.position = position;
      this.velocity = velocity;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof State)) return false;
      State other = (State) obj;
      return Math.abs(position - other.position) < 1e-9
          && Math.abs(velocity - other.velocity) < 1e-9;
    }

    @Override
    public int hashCode() {
      return Double.hashCode(position) * 31 + Double.hashCode(velocity);
    }
  }

  private final Constraints constraints;
  private State current;
  private final State goal;

  /**
   * Constructs a new TrapezoidProfile.
   *
   * @param constraints The velocity and acceleration constraints.
   * @param goal The desired end state.
   * @param initial The starting state.
   */
  public TrapezoidProfile(Constraints constraints, State goal, State initial) {
    this.constraints = constraints;
    this.goal = new State(goal.position, goal.velocity);
    this.current = new State(initial.position, initial.velocity);
  }

  /**
   * Constructs a profile that starts from rest.
   *
   * @param constraints The velocity and acceleration constraints.
   * @param goal The desired end state.
   */
  public TrapezoidProfile(Constraints constraints, State goal) {
    this(constraints, goal, new State(0, 0));
  }

  /**
   * Advances the profile by the given time delta and returns the new state.
   *
   * @param dt The time step in seconds (typically 0.02 for a 50Hz loop).
   * @return The profiled state at the new time.
   */
  public State calculate(double dt) {
    if (dt <= 0) return current;

    double direction = Math.signum(goal.position - current.position);

    // If we're close enough and velocity is near zero, snap to goal
    if (Math.abs(goal.position - current.position) < 1e-9 && Math.abs(current.velocity) < 1e-9) {
      current.position = goal.position;
      current.velocity = goal.velocity;
      return current;
    }

    // Handle direction: work in the positive direction, flip at the end
    double distanceToGo = Math.abs(goal.position - current.position);

    // Velocity needed to decelerate to goal velocity from current v
    // v² = v_goal² + 2*a*d  =>  v_decel = sqrt(v_goal² + 2*a*d)
    double maxReachableVelocity =
        Math.sqrt(goal.velocity * goal.velocity + 2.0 * constraints.maxAcceleration * distanceToGo);

    // Constrain to max velocity
    double targetVelocity = Math.min(maxReachableVelocity, constraints.maxVelocity);
    targetVelocity *= direction;

    // Accelerate towards target velocity
    double velocityError = targetVelocity - current.velocity;
    double maxVelocityStep = constraints.maxAcceleration * dt;
    double newVelocity =
        current.velocity + Math.max(-maxVelocityStep, Math.min(maxVelocityStep, velocityError));

    // Integrate position
    current.position += newVelocity * dt;
    current.velocity = newVelocity;

    // Prevent overshooting the goal
    if (direction > 0 && current.position > goal.position) {
      current.position = goal.position;
      current.velocity = goal.velocity;
    } else if (direction < 0 && current.position < goal.position) {
      current.position = goal.position;
      current.velocity = goal.velocity;
    }

    return current;
  }

  /**
   * Returns the current state of the profile.
   *
   * @return The current state.
   */
  public State getState() {
    return current;
  }

  /**
   * Returns the goal state.
   *
   * @return The goal state.
   */
  public State getGoal() {
    return goal;
  }

  /**
   * Returns true if the profile has reached the goal state.
   *
   * @return true if finished, false otherwise.
   */
  public boolean isFinished() {
    return Math.abs(current.position - goal.position) < 1e-6
        && Math.abs(current.velocity - goal.velocity) < 1e-6;
  }
}

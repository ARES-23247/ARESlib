package org.areslib.pathplanner.dummy;

/**
 * A dummy TrapezoidProfile fallback implementation since ARESLib2 relies on native math profiles.
 */
public class TrapezoidProfile {
  public static class Constraints {
    public final double maxVelocity;
    public final double maxAcceleration;

    public Constraints(double maxV, double maxA) {
      this.maxVelocity = maxV;
      this.maxAcceleration = maxA;
    }
  }

  public static class State {
    public double position;
    public double velocity;

    public State() {}

    public State(double p, double v) {
      position = p;
      velocity = v;
    }
  }

  public TrapezoidProfile(Constraints c) {}

  public State calculate(double t, State current, State goal) {
    return new State();
  }

  public double timeLeftUntil(double t) {
    return 0;
  }
}

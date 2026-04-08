package org.areslib.math.geometry;

import java.util.Objects;

/**
 * A change in distance along an arc since the last pose update. We can use ideas from differential
 * calculus to create new Pose2ds from a Twist2d and vice versa.
 *
 * <p>A Twist can be used to represent a difference between two poses.
 */
public class Twist2d {
  /** Linear "dx" component. */
  public double dx;

  /** Linear "dy" component. */
  public double dy;

  /** Angular "dtheta" component (radians). */
  public double dtheta;

  /**
   * Constructs a Twist2d with the given values.
   *
   * @param dx Change in x direction relative to robot.
   * @param dy Change in y direction relative to robot.
   * @param dtheta Change in angle (radians).
   */
  public Twist2d(double dx, double dy, double dtheta) {
    this.dx = dx;
    this.dy = dy;
    this.dtheta = dtheta;
  }

  /** Constructs a Twist2d with zero values. */
  public Twist2d() {
    this(0.0, 0.0, 0.0);
  }

  /**
   * Scale this twist by a scalar.
   *
   * @param scalar The scalar.
   * @return The scaled twist.
   */
  public Twist2d scaled(double scalar) {
    return new Twist2d(dx * scalar, dy * scalar, dtheta * scalar);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Twist2d other = (Twist2d) obj;
    return Math.abs(other.dx - dx) < 1e-9
        && Math.abs(other.dy - dy) < 1e-9
        && Math.abs(other.dtheta - dtheta) < 1e-9;
  }

  @Override
  public int hashCode() {
    // Round to 1e-9 to match the epsilon tolerance used in equals().
    return Objects.hash(Math.round(dx * 1e9), Math.round(dy * 1e9), Math.round(dtheta * 1e9));
  }
}

package org.areslib.math.geometry;

import java.util.Objects;

/**
 * Represents a translation in 2D space. This object can be used to represent a point or a vector.
 *
 * <p>This assumes that you are using standard FTC coordinate systems (x is forward, y is left).
 */
public class Translation2d implements Interpolatable<Translation2d> {
  private double x;
  private double y;

  public Translation2d() {
    x = 0.0;
    y = 0.0;
  }

  public Translation2d(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public Translation2d(double distance, Rotation2d angle) {
    x = distance * angle.getCos();
    y = distance * angle.getSin();
  }

  /**
   * Sets the coordinates of the Translation2d in-place. Useful for eliminating garbage collection
   * overhead in tight loops.
   */
  public void set(double x, double y) {
    this.x = x;
    this.y = y;
  }

  /** Sets the coordinates equal to another Translation2d in-place. */
  public void set(Translation2d other) {
    x = other.x;
    y = other.y;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getDistance(Translation2d other) {
    return Math.hypot(other.getX() - x, other.getY() - y);
  }

  public double getNorm() {
    return Math.hypot(x, y);
  }

  public Rotation2d getAngle() {
    return new Rotation2d(x, y);
  }

  public Translation2d rotateBy(Rotation2d other) {
    return new Translation2d(
        x * other.getCos() - y * other.getSin(), x * other.getSin() + y * other.getCos());
  }

  public Translation2d plus(Translation2d other) {
    return new Translation2d(x + other.x, y + other.y);
  }

  public Translation2d minus(Translation2d other) {
    return new Translation2d(x - other.x, y - other.y);
  }

  public Translation2d unaryMinus() {
    return new Translation2d(-x, -y);
  }

  public Translation2d times(double scalar) {
    return new Translation2d(x * scalar, y * scalar);
  }

  public Translation2d div(double scalar) {
    return new Translation2d(x / scalar, y / scalar);
  }

  @Override
  public Translation2d interpolate(Translation2d endValue, double t) {
    if (t <= 0) return this;
    if (t >= 1) return endValue;
    return new Translation2d(x + (endValue.x - x) * t, y + (endValue.y - y) * t);
  }

  @Override
  public String toString() {
    return String.format("Translation2d(X: %.2f, Y: %.2f)", x, y);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Translation2d)) return false;
    Translation2d other = (Translation2d) obj;
    return Math.abs(other.x - x) < 1e-9 && Math.abs(other.y - y) < 1e-9;
  }

  @Override
  public int hashCode() {
    // Round to 1e-9 to match the epsilon tolerance used in equals().
    // Two Translation2d values that are equals() MUST produce the same hash.
    long xHash = Math.round(x * 1e9);
    long yHash = Math.round(y * 1e9);
    return Objects.hash(xHash, yHash);
  }
}

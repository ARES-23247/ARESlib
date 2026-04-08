package org.areslib.math.geometry;

import java.util.Objects;

/** A rotation in a 2D coordinate frame represented a point on the unit circle (cosine and sine). */
public class Rotation2d implements Interpolatable<Rotation2d> {
  private final double m_value;
  private final double m_cos;
  private final double m_sin;

  public Rotation2d() {
    m_value = 0.0;
    m_cos = 1.0;
    m_sin = 0.0;
  }

  public Rotation2d(double value) {
    m_value = value;
    m_cos = Math.cos(value);
    m_sin = Math.sin(value);
  }

  public Rotation2d(double x, double y) {
    double magnitude = Math.hypot(x, y);
    if (magnitude > 1e-6) {
      m_sin = y / magnitude;
      m_cos = x / magnitude;
    } else {
      m_sin = 0.0;
      m_cos = 1.0;
    }
    m_value = Math.atan2(m_sin, m_cos);
  }

  public static Rotation2d fromDegrees(double degrees) {
    return new Rotation2d(Math.toRadians(degrees));
  }

  public double getRadians() {
    return m_value;
  }

  public double getDegrees() {
    return Math.toDegrees(m_value);
  }

  public double getCos() {
    return m_cos;
  }

  public double getSin() {
    return m_sin;
  }

  public Rotation2d plus(Rotation2d other) {
    return rotateBy(other);
  }

  public Rotation2d minus(Rotation2d other) {
    return rotateBy(other.unaryMinus());
  }

  public Rotation2d unaryMinus() {
    return new Rotation2d(-m_value);
  }

  public Rotation2d times(double scalar) {
    return new Rotation2d(m_value * scalar);
  }

  public Rotation2d rotateBy(Rotation2d other) {
    return new Rotation2d(
        m_cos * other.m_cos - m_sin * other.m_sin, m_cos * other.m_sin + m_sin * other.m_cos);
  }

  @Override
  public Rotation2d interpolate(Rotation2d endValue, double t) {
    if (t <= 0) return this;
    if (t >= 1) return endValue;
    double diff = endValue.minus(this).getRadians();
    return this.plus(new Rotation2d(diff * t));
  }

  @Override
  public String toString() {
    return String.format("Rotation2d(Rads: %.2f, Deg: %.2f)", m_value, Math.toDegrees(m_value));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Rotation2d)) return false;
    Rotation2d other = (Rotation2d) obj;
    return Math.hypot(m_cos - other.m_cos, m_sin - other.m_sin) < 1e-9;
  }

  @Override
  public int hashCode() {
    // Round to 1e-9 to match the epsilon tolerance used in equals().
    // Two Rotation2d values that are equals() MUST produce the same hash.
    long cosHash = Math.round(m_cos * 1e9);
    long sinHash = Math.round(m_sin * 1e9);
    return Objects.hash(cosHash, sinHash);
  }
}

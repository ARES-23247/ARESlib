package org.areslib.math.geometry;

import java.util.Objects;

/** Represents a 2D pose containing translational and rotational elements. */
public class Pose2d implements Interpolatable<Pose2d> {
  private final Translation2d m_translation;
  private final Rotation2d m_rotation;

  public Pose2d() {
    m_translation = new Translation2d();
    m_rotation = new Rotation2d();
  }

  public Pose2d(Translation2d translation, Rotation2d rotation) {
    m_translation = translation;
    m_rotation = rotation;
  }

  public Pose2d(double x, double y, Rotation2d rotation) {
    m_translation = new Translation2d(x, y);
    m_rotation = rotation;
  }

  public Transform2d minus(Pose2d other) {
    return new Transform2d(other, this);
  }

  public Translation2d getTranslation() {
    return m_translation;
  }

  public double getX() {
    return m_translation.getX();
  }

  public double getY() {
    return m_translation.getY();
  }

  public Rotation2d getRotation() {
    return m_rotation;
  }

  public Pose2d plus(Pose2d other) {
    return transformBy(other);
  }

  public Pose2d transformBy(Pose2d other) {
    return new Pose2d(
        m_translation.plus(other.m_translation.rotateBy(m_rotation)),
        m_rotation.plus(other.m_rotation));
  }

  public Pose2d relativeTo(Pose2d other) {
    Translation2d transform =
        m_translation.minus(other.m_translation).rotateBy(other.m_rotation.unaryMinus());
    Rotation2d rotation = m_rotation.minus(other.m_rotation);
    return new Pose2d(transform, rotation);
  }

  @Override
  public Pose2d interpolate(Pose2d endValue, double t) {
    if (t <= 0) return this;
    if (t >= 1) return endValue;

    Twist2d twist = this.log(endValue);
    return this.exp(twist.scaled(t));
  }

  @Override
  public String toString() {
    return String.format("Pose2d(%s, %s)", m_translation.toString(), m_rotation.toString());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Pose2d)) return false;
    Pose2d other = (Pose2d) obj;
    return m_translation.equals(other.m_translation) && m_rotation.equals(other.m_rotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_translation, m_rotation);
  }

  /**
   * Obtain a new Pose2d from a (constant curvature) velocity.
   *
   * <p>See <a href="https://file.tavsys.net/control/controls-engineering-in-frc.pdf">Controls
   * Engineering in the FIRST Robotics Competition</a> section 10.2 "Pose exponential" for a
   * derivation.
   *
   * @param twist The twist to map to a Pose2d.
   * @return The new Pose2d.
   */
  public Pose2d exp(Twist2d twist) {
    double dx = twist.dx;
    double dy = twist.dy;
    double dtheta = twist.dtheta;

    double sinTheta = Math.sin(dtheta);
    double cosTheta = Math.cos(dtheta);

    double s, c;
    if (Math.abs(dtheta) < 1E-9) {
      s = 1.0 - 1.0 / 6.0 * dtheta * dtheta;
      c = 0.5 * dtheta;
    } else {
      s = sinTheta / dtheta;
      c = (1.0 - cosTheta) / dtheta;
    }

    Transform2d transform =
        new Transform2d(
            new Translation2d(dx * s - dy * c, dx * c + dy * s),
            new Rotation2d(cosTheta, sinTheta));

    return this.plus(new Pose2d(transform.getTranslation(), transform.getRotation()));
  }

  /**
   * Returns a Twist2d that maps this pose to the end pose.
   *
   * <p>If c is the cosine of dtheta and s is the sine of dtheta, and the transformation is given by
   * (x, y, theta), then the velocity vector (dx, dy, dtheta) can be computed.
   *
   * @param end The end pose.
   * @return The twist that maps this pose to the end pose.
   */
  public Twist2d log(Pose2d end) {
    Pose2d transform = end.relativeTo(this);
    double dtheta = transform.getRotation().getRadians();
    double halfDtheta = 0.5 * dtheta;
    double cosMinusOne = transform.getRotation().getCos() - 1.0;

    double halfThetaByTanOfHalfDtheta;
    if (Math.abs(cosMinusOne) < 1E-9) {
      halfThetaByTanOfHalfDtheta = 1.0 - 1.0 / 12.0 * dtheta * dtheta;
    } else {
      halfThetaByTanOfHalfDtheta = -(halfDtheta * transform.getRotation().getSin()) / cosMinusOne;
    }

    Translation2d translationPart =
        transform
            .getTranslation()
            .rotateBy(new Rotation2d(halfThetaByTanOfHalfDtheta, -halfDtheta));

    return new Twist2d(translationPart.getX(), translationPart.getY(), dtheta);
  }
}

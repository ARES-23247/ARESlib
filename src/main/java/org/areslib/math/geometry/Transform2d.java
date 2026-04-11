package org.areslib.math.geometry;

import java.util.Objects;

/**
 * Represents a transformation for a Pose2d in the cartesian coordinate plane. Useful for
 * representing a rigid body displacement.
 */
public class Transform2d {
  private final Translation2d translation;
  private final Rotation2d rotation;

  /**
   * Constructs the transform that maps the initial pose to the final pose.
   *
   * @param initial The initial pose.
   * @param last The final pose.
   */
  public Transform2d(Pose2d initial, Pose2d last) {
    // We are rotating the difference between the translations
    // using the inverse of the first rotation.
    translation =
        last.getTranslation()
            .minus(initial.getTranslation())
            .rotateBy(initial.getRotation().unaryMinus());

    rotation = last.getRotation().minus(initial.getRotation());
  }

  /**
   * Constructs a transform with the given translation and rotation components.
   *
   * @param translation Translational component of the transform.
   * @param rotation Rotational component of the transform.
   */
  public Transform2d(Translation2d translation, Rotation2d rotation) {
    this.translation = translation;
    this.rotation = rotation;
  }

  /** Constructs the identity transform -- an empty displacement. */
  public Transform2d() {
    translation = new Translation2d();
    rotation = new Rotation2d();
  }

  /**
   * Multiplies the transform by the scalar.
   *
   * @param scalar The scalar.
   * @return The scaled transform.
   */
  public Transform2d times(double scalar) {
    return new Transform2d(translation.times(scalar), rotation.times(scalar));
  }

  /**
   * Gets the translation component of the transform.
   *
   * @return The translational component of the transform.
   */
  public Translation2d getTranslation() {
    return translation;
  }

  /**
   * Gets the rotational component of the transform.
   *
   * @return The rotational component of the transform.
   */
  public Rotation2d getRotation() {
    return rotation;
  }

  /**
   * Invert the transformation. This is useful for "undoing" a transformation.
   *
   * @return The inverted transformation.
   */
  public Transform2d inverse() {
    // Rotate the inverted translation back by the inverted rotation
    return new Transform2d(
        getTranslation().unaryMinus().rotateBy(getRotation().unaryMinus()),
        getRotation().unaryMinus());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Transform2d)) return false;
    Transform2d other = (Transform2d) obj;
    return translation.equals(other.translation) && rotation.equals(other.rotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(translation, rotation);
  }
}

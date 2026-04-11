package org.areslib.math.geometry;

/**
 * A 3D translation (vector) with x, y, and z components in meters.
 *
 * <p>Used for representing 3D positions of cameras, AprilTags, and robot components in 3D
 * coordinate space.
 */
public class Translation3d {
  private final double x;
  private final double y;
  private final double z;

  /** Constructs the zero translation. */
  public Translation3d() {
    this(0.0, 0.0, 0.0);
  }

  /**
   * Constructs a Translation3d with the given components.
   *
   * @param x The x component in meters.
   * @param y The y component in meters.
   * @param z The z component in meters.
   */
  public Translation3d(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  // ── Accessors ──────────────────────────────────────────────────────────────

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  /**
   * Returns the Euclidean norm (length) of the translation vector.
   *
   * @return The norm of the translation.
   */
  public double getNorm() {
    return Math.sqrt(x * x + y * y + z * z);
  }

  // ── Operations ─────────────────────────────────────────────────────────────

  /**
   * Returns the sum of this translation and another.
   *
   * @param other The translation to add.
   * @return The sum translation.
   */
  public Translation3d plus(Translation3d other) {
    return new Translation3d(x + other.x, y + other.y, z + other.z);
  }

  /**
   * Returns the difference of this translation and another.
   *
   * @param other The translation to subtract.
   * @return The difference translation.
   */
  public Translation3d minus(Translation3d other) {
    return new Translation3d(x - other.x, y - other.y, z - other.z);
  }

  /**
   * Returns this translation scaled by a scalar.
   *
   * @param scalar The scalar to multiply by.
   * @return The scaled translation.
   */
  public Translation3d times(double scalar) {
    return new Translation3d(x * scalar, y * scalar, z * scalar);
  }

  /**
   * Returns the negation of this translation.
   *
   * @return The negated translation.
   */
  public Translation3d unaryMinus() {
    return new Translation3d(-x, -y, -z);
  }

  /**
   * Rotates this translation by the given 3D rotation.
   *
   * <p>Uses the quaternion rotation formula: v' = q * v * q^(-1)
   *
   * @param rotation The rotation to apply.
   * @return The rotated translation.
   */
  public Translation3d rotateBy(Rotation3d rotation) {
    double w = rotation.getW();
    double qx = rotation.getX();
    double qy = rotation.getY();
    double qz = rotation.getZ();

    // Rodrigues' rotation via quaternion: p' = q * p * q*
    // Optimized form avoiding full quaternion multiply:
    double tx = 2.0 * (qy * z - qz * y);
    double ty = 2.0 * (qz * x - qx * z);
    double tz = 2.0 * (qx * y - qy * x);

    return new Translation3d(
        x + w * tx + (qy * tz - qz * ty),
        y + w * ty + (qz * tx - qx * tz),
        z + w * tz + (qx * ty - qy * tx));
  }

  /**
   * Projects this 3D translation to a 2D translation by discarding the Z component.
   *
   * @return The 2D translation.
   */
  public Translation2d toTranslation2d() {
    return new Translation2d(x, y);
  }

  // ── Object overrides ───────────────────────────────────────────────────────

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Translation3d)) return false;
    Translation3d other = (Translation3d) obj;
    return Math.abs(x - other.x) < 1e-9
        && Math.abs(y - other.y) < 1e-9
        && Math.abs(z - other.z) < 1e-9;
  }

  @Override
  public int hashCode() {
    long xBits = Double.doubleToLongBits(Math.round(x * 1e9) / 1e9);
    long yBits = Double.doubleToLongBits(Math.round(y * 1e9) / 1e9);
    long zBits = Double.doubleToLongBits(Math.round(z * 1e9) / 1e9);
    return Long.hashCode(xBits) * 31 * 31 + Long.hashCode(yBits) * 31 + Long.hashCode(zBits);
  }

  @Override
  public String toString() {
    return String.format("Translation3d(x=%.4f, y=%.4f, z=%.4f)", x, y, z);
  }
}

package org.areslib.math.geometry;

/**
 * A rotation in 3D space represented by a unit quaternion (w + xi + yj + zk).
 *
 * <p>Quaternions avoid gimbal lock and provide smooth interpolation. This class is used for
 * AprilTag 3D pose estimation, camera mount transforms, and AdvantageScope 3D field visualization.
 */
public class Rotation3d {
  private final double m_w;
  private final double m_x;
  private final double m_y;
  private final double m_z;

  /** Constructs the identity rotation (no rotation). */
  public Rotation3d() {
    m_w = 1.0;
    m_x = 0.0;
    m_y = 0.0;
    m_z = 0.0;
  }

  /**
   * Constructs a Rotation3d from a unit quaternion. The quaternion will be normalized if necessary.
   *
   * @param w The scalar (real) component.
   * @param x The x component of the vector part.
   * @param y The y component of the vector part.
   * @param z The z component of the vector part.
   */
  public Rotation3d(double w, double x, double y, double z) {
    double norm = Math.sqrt(w * w + x * x + y * y + z * z);
    if (norm < 1e-12) {
      // Degenerate — default to identity
      m_w = 1.0;
      m_x = 0.0;
      m_y = 0.0;
      m_z = 0.0;
    } else {
      m_w = w / norm;
      m_x = x / norm;
      m_y = y / norm;
      m_z = z / norm;
    }
  }

  /**
   * Constructs a Rotation3d from extrinsic roll, pitch, and yaw Euler angles.
   *
   * <p>This uses the XYZ convention: roll around X, then pitch around Y, then yaw around Z.
   *
   * @param roll Rotation around the X axis in radians.
   * @param pitch Rotation around the Y axis in radians.
   * @param yaw Rotation around the Z axis in radians.
   */
  public Rotation3d(double roll, double pitch, double yaw) {
    // Convert Euler angles to quaternion via half-angle formulas
    double cr = Math.cos(roll * 0.5);
    double sr = Math.sin(roll * 0.5);
    double cp = Math.cos(pitch * 0.5);
    double sp = Math.sin(pitch * 0.5);
    double cy = Math.cos(yaw * 0.5);
    double sy = Math.sin(yaw * 0.5);

    double w = cr * cp * cy + sr * sp * sy;
    double x = sr * cp * cy - cr * sp * sy;
    double y = cr * sp * cy + sr * cp * sy;
    double z = cr * cp * sy - sr * sp * cy;

    double norm = Math.sqrt(w * w + x * x + y * y + z * z);
    m_w = w / norm;
    m_x = x / norm;
    m_y = y / norm;
    m_z = z / norm;
  }

  // ── Accessors ──────────────────────────────────────────────────────────────

  public double getW() {
    return m_w;
  }

  public double getX() {
    return m_x;
  }

  public double getY() {
    return m_y;
  }

  public double getZ() {
    return m_z;
  }

  /**
   * Returns the roll (rotation around X) in radians.
   *
   * @return The roll in radians.
   */
  public double getRoll() {
    double sinr = 2.0 * (m_w * m_x + m_y * m_z);
    double cosr = 1.0 - 2.0 * (m_x * m_x + m_y * m_y);
    return Math.atan2(sinr, cosr);
  }

  /**
   * Returns the pitch (rotation around Y) in radians.
   *
   * @return The pitch in radians.
   */
  public double getPitch() {
    double sinp = 2.0 * (m_w * m_y - m_z * m_x);
    // Clamp to avoid NaN from floating point drift
    sinp = Math.max(-1.0, Math.min(1.0, sinp));
    return Math.asin(sinp);
  }

  /**
   * Returns the yaw (rotation around Z) in radians.
   *
   * @return The yaw in radians.
   */
  public double getYaw() {
    double siny = 2.0 * (m_w * m_z + m_x * m_y);
    double cosy = 1.0 - 2.0 * (m_y * m_y + m_z * m_z);
    return Math.atan2(siny, cosy);
  }

  // ── Operations ─────────────────────────────────────────────────────────────

  /**
   * Composes this rotation with another (Hamilton product).
   *
   * @param other The rotation to apply after this one.
   * @return The composed rotation.
   */
  public Rotation3d rotateBy(Rotation3d other) {
    return new Rotation3d(
        m_w * other.m_w - m_x * other.m_x - m_y * other.m_y - m_z * other.m_z,
        m_w * other.m_x + m_x * other.m_w + m_y * other.m_z - m_z * other.m_y,
        m_w * other.m_y - m_x * other.m_z + m_y * other.m_w + m_z * other.m_x,
        m_w * other.m_z + m_x * other.m_y - m_y * other.m_x + m_z * other.m_w);
  }

  /**
   * Returns the inverse of this rotation (conjugate of the unit quaternion).
   *
   * @return The inverse rotation.
   */
  public Rotation3d unaryMinus() {
    return new Rotation3d(m_w, -m_x, -m_y, -m_z);
  }

  /**
   * Projects this 3D rotation down to a 2D rotation by extracting the yaw component.
   *
   * @return The 2D rotation.
   */
  public Rotation2d toRotation2d() {
    return new Rotation2d(getYaw());
  }

  // ── Object overrides ───────────────────────────────────────────────────────

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Rotation3d)) return false;
    Rotation3d other = (Rotation3d) obj;
    // Two quaternions q and -q represent the same rotation
    double dot = m_w * other.m_w + m_x * other.m_x + m_y * other.m_y + m_z * other.m_z;
    return Math.abs(Math.abs(dot) - 1.0) < 1e-9;
  }

  @Override
  public int hashCode() {
    // Canonical form: ensure w >= 0
    double sign = m_w >= 0 ? 1.0 : -1.0;
    long wBits = Double.doubleToLongBits(Math.round(sign * m_w * 1e9) / 1e9);
    long xBits = Double.doubleToLongBits(Math.round(sign * m_x * 1e9) / 1e9);
    long yBits = Double.doubleToLongBits(Math.round(sign * m_y * 1e9) / 1e9);
    long zBits = Double.doubleToLongBits(Math.round(sign * m_z * 1e9) / 1e9);
    return Long.hashCode(wBits) * 31 * 31 * 31
        + Long.hashCode(xBits) * 31 * 31
        + Long.hashCode(yBits) * 31
        + Long.hashCode(zBits);
  }

  @Override
  public String toString() {
    return String.format("Rotation3d(w=%.4f, x=%.4f, y=%.4f, z=%.4f)", m_w, m_x, m_y, m_z);
  }
}

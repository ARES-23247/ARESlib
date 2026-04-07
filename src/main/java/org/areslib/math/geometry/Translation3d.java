package org.areslib.math.geometry;

/**
 * A 3D translation (vector) with x, y, and z components in meters.
 * <p>
 * Used for representing 3D positions of cameras, AprilTags, and robot components
 * in 3D coordinate space.
 */
public class Translation3d {
    private final double m_x;
    private final double m_y;
    private final double m_z;

    /**
     * Constructs the zero translation.
     */
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
        m_x = x;
        m_y = y;
        m_z = z;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public double getX() { return m_x; }
    public double getY() { return m_y; }
    public double getZ() { return m_z; }

    /**
     * Returns the Euclidean norm (length) of the translation vector.
     *
     * @return The norm of the translation.
     */
    public double getNorm() {
        return Math.sqrt(m_x * m_x + m_y * m_y + m_z * m_z);
    }

    // ── Operations ─────────────────────────────────────────────────────────────

    /**
     * Returns the sum of this translation and another.
     *
     * @param other The translation to add.
     * @return The sum translation.
     */
    public Translation3d plus(Translation3d other) {
        return new Translation3d(m_x + other.m_x, m_y + other.m_y, m_z + other.m_z);
    }

    /**
     * Returns the difference of this translation and another.
     *
     * @param other The translation to subtract.
     * @return The difference translation.
     */
    public Translation3d minus(Translation3d other) {
        return new Translation3d(m_x - other.m_x, m_y - other.m_y, m_z - other.m_z);
    }

    /**
     * Returns this translation scaled by a scalar.
     *
     * @param scalar The scalar to multiply by.
     * @return The scaled translation.
     */
    public Translation3d times(double scalar) {
        return new Translation3d(m_x * scalar, m_y * scalar, m_z * scalar);
    }

    /**
     * Returns the negation of this translation.
     *
     * @return The negated translation.
     */
    public Translation3d unaryMinus() {
        return new Translation3d(-m_x, -m_y, -m_z);
    }

    /**
     * Rotates this translation by the given 3D rotation.
     * <p>
     * Uses the quaternion rotation formula: v' = q * v * q^(-1)
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
        double tx = 2.0 * (qy * m_z - qz * m_y);
        double ty = 2.0 * (qz * m_x - qx * m_z);
        double tz = 2.0 * (qx * m_y - qy * m_x);

        return new Translation3d(
            m_x + w * tx + (qy * tz - qz * ty),
            m_y + w * ty + (qz * tx - qx * tz),
            m_z + w * tz + (qx * ty - qy * tx)
        );
    }

    /**
     * Projects this 3D translation to a 2D translation by discarding the Z component.
     *
     * @return The 2D translation.
     */
    public Translation2d toTranslation2d() {
        return new Translation2d(m_x, m_y);
    }

    // ── Object overrides ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Translation3d)) return false;
        Translation3d other = (Translation3d) obj;
        return Math.abs(m_x - other.m_x) < 1e-9
            && Math.abs(m_y - other.m_y) < 1e-9
            && Math.abs(m_z - other.m_z) < 1e-9;
    }

    @Override
    public int hashCode() {
        long xBits = Double.doubleToLongBits(Math.round(m_x * 1e9) / 1e9);
        long yBits = Double.doubleToLongBits(Math.round(m_y * 1e9) / 1e9);
        long zBits = Double.doubleToLongBits(Math.round(m_z * 1e9) / 1e9);
        return Long.hashCode(xBits) * 31 * 31 + Long.hashCode(yBits) * 31 + Long.hashCode(zBits);
    }

    @Override
    public String toString() {
        return String.format("Translation3d(x=%.4f, y=%.4f, z=%.4f)", m_x, m_y, m_z);
    }
}

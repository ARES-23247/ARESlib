package org.areslib.math.geometry;

/**
 * A 3D spatial transform consisting of a translation and rotation offset.
 * <p>
 * Represents a rigid body displacement in 3D space. Used to describe camera mount
 * positions relative to the robot center, or the relative offset between two 3D poses.
 */
public class Transform3d {
    private final Translation3d m_translation;
    private final Rotation3d m_rotation;

    /**
     * Constructs the identity transform (no offset).
     */
    public Transform3d() {
        this(new Translation3d(), new Rotation3d());
    }

    /**
     * Constructs a Transform3d from a translation and rotation.
     *
     * @param translation The translation component.
     * @param rotation The rotation component.
     */
    public Transform3d(Translation3d translation, Rotation3d rotation) {
        m_translation = translation;
        m_rotation = rotation;
    }

    /**
     * Constructs a Transform3d that maps one pose to another.
     *
     * @param from The initial pose.
     * @param to   The final pose.
     */
    public Transform3d(Pose3d from, Pose3d to) {
        Transform3d relative = to.relativeTo(from);
        m_translation = relative.m_translation;
        m_rotation = relative.m_rotation;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public Translation3d getTranslation() { return m_translation; }
    public Rotation3d getRotation() { return m_rotation; }
    public double getX() { return m_translation.getX(); }
    public double getY() { return m_translation.getY(); }
    public double getZ() { return m_translation.getZ(); }

    // ── Operations ─────────────────────────────────────────────────────────────

    /**
     * Composes this transform with another.
     *
     * @param other The transform to compose with.
     * @return The composed transform.
     */
    public Transform3d plus(Transform3d other) {
        Pose3d origin = new Pose3d();
        return origin.transformBy(this).transformBy(other).relativeTo(origin);
    }

    /**
     * Returns the inverse of this transform.
     *
     * @return The inverse transform.
     */
    public Transform3d inverse() {
        Rotation3d invRotation = m_rotation.unaryMinus();
        Translation3d invTranslation = m_translation.unaryMinus().rotateBy(invRotation);
        return new Transform3d(invTranslation, invRotation);
    }

    // ── Object overrides ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Transform3d)) return false;
        Transform3d other = (Transform3d) obj;
        return m_translation.equals(other.m_translation) && m_rotation.equals(other.m_rotation);
    }

    @Override
    public int hashCode() {
        return m_translation.hashCode() * 31 + m_rotation.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Transform3d(%s, %s)", m_translation, m_rotation);
    }
}

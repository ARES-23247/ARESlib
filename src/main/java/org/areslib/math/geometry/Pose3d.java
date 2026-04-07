package org.areslib.math.geometry;

/**
 * A 3D pose consisting of a 3D translation and a 3D rotation.
 * <p>
 * Used for representing the full 6-DOF position and orientation of cameras,
 * AprilTags, and robots in 3D space. Supports transformation composition and
 * projection to 2D for field-relative operations.
 */
public class Pose3d {
    private final Translation3d m_translation;
    private final Rotation3d m_rotation;

    /**
     * Constructs the identity pose at the origin.
     */
    public Pose3d() {
        this(new Translation3d(), new Rotation3d());
    }

    /**
     * Constructs a Pose3d from a translation and rotation.
     */
    public Pose3d(Translation3d translation, Rotation3d rotation) {
        m_translation = translation;
        m_rotation = rotation;
    }

    /**
     * Constructs a Pose3d from individual components.
     *
     * @param x     X position in meters.
     * @param y     Y position in meters.
     * @param z     Z position in meters.
     * @param rotation The 3D rotation.
     */
    public Pose3d(double x, double y, double z, Rotation3d rotation) {
        this(new Translation3d(x, y, z), rotation);
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public Translation3d getTranslation() { return m_translation; }
    public Rotation3d getRotation() { return m_rotation; }
    public double getX() { return m_translation.getX(); }
    public double getY() { return m_translation.getY(); }
    public double getZ() { return m_translation.getZ(); }

    // ── Operations ─────────────────────────────────────────────────────────────

    /**
     * Applies a Transform3d to this pose.
     * <p>
     * The transform is applied as: new_translation = old_translation + rotation * transform.translation,
     * new_rotation = old_rotation * transform.rotation
     *
     * @param transform The transform to apply.
     * @return The transformed pose.
     */
    public Pose3d transformBy(Transform3d transform) {
        Translation3d rotatedTranslation = transform.getTranslation().rotateBy(m_rotation);
        return new Pose3d(
            m_translation.plus(rotatedTranslation),
            m_rotation.rotateBy(transform.getRotation())
        );
    }

    /**
     * Computes the transform that maps the other pose to this pose.
     * <p>
     * Equivalent to: this = other.transformBy(result)
     *
     * @param other The reference pose.
     * @return The relative Transform3d from other to this.
     */
    public Transform3d relativeTo(Pose3d other) {
        Translation3d delta = m_translation.minus(other.m_translation)
                .rotateBy(other.m_rotation.unaryMinus());
        Rotation3d deltaRotation = other.m_rotation.unaryMinus().rotateBy(m_rotation);
        return new Transform3d(delta, deltaRotation);
    }

    /**
     * Projects this 3D pose to a 2D pose by extracting (x, y, yaw).
     * <p>
     * This is the primary method for converting AprilTag 3D detections to field-plane poses.
     */
    public Pose2d toPose2d() {
        return new Pose2d(
            m_translation.getX(),
            m_translation.getY(),
            m_rotation.toRotation2d()
        );
    }

    // ── Object overrides ───────────────────────────────────────────────────────

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Pose3d)) return false;
        Pose3d other = (Pose3d) obj;
        return m_translation.equals(other.m_translation) && m_rotation.equals(other.m_rotation);
    }

    @Override
    public int hashCode() {
        return m_translation.hashCode() * 31 + m_rotation.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Pose3d(%s, %s)", m_translation, m_rotation);
    }
}

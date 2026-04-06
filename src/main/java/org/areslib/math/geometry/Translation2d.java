package org.areslib.math.geometry;

import java.util.Objects;

/**
 * Represents a translation in 2D space.
 * This object can be used to represent a point or a vector.
 *
 * This assumes that you are using standard FTC coordinate systems 
 * (x is forward, y is left).
 */
public class Translation2d implements Interpolatable<Translation2d> {
    private final double m_x;
    private final double m_y;

    public Translation2d() {
        m_x = 0.0;
        m_y = 0.0;
    }

    public Translation2d(double x, double y) {
        m_x = x;
        m_y = y;
    }

    public Translation2d(double distance, Rotation2d angle) {
        m_x = distance * angle.getCos();
        m_y = distance * angle.getSin();
    }

    public double getX() { return m_x; }
    public double getY() { return m_y; }

    public double getNorm() {
        return Math.hypot(m_x, m_y);
    }

    public Rotation2d getAngle() {
        return new Rotation2d(m_x, m_y);
    }

    public Translation2d rotateBy(Rotation2d other) {
        return new Translation2d(
            m_x * other.getCos() - m_y * other.getSin(),
            m_x * other.getSin() + m_y * other.getCos()
        );
    }

    public Translation2d plus(Translation2d other) {
        return new Translation2d(m_x + other.m_x, m_y + other.m_y);
    }

    public Translation2d minus(Translation2d other) {
        return new Translation2d(m_x - other.m_x, m_y - other.m_y);
    }

    public Translation2d unaryMinus() {
        return new Translation2d(-m_x, -m_y);
    }

    public Translation2d times(double scalar) {
        return new Translation2d(m_x * scalar, m_y * scalar);
    }

    public Translation2d div(double scalar) {
        return new Translation2d(m_x / scalar, m_y / scalar);
    }

    @Override
    public Translation2d interpolate(Translation2d endValue, double t) {
        if (t <= 0) return this;
        if (t >= 1) return endValue;
        return new Translation2d(
            m_x + (endValue.m_x - m_x) * t,
            m_y + (endValue.m_y - m_y) * t
        );
    }

    @Override
    public String toString() {
        return String.format("Translation2d(X: %.2f, Y: %.2f)", m_x, m_y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Translation2d)) return false;
        Translation2d other = (Translation2d) obj;
        return Math.abs(other.m_x - m_x) < 1e-9 && Math.abs(other.m_y - m_y) < 1e-9;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_x, m_y);
    }
}

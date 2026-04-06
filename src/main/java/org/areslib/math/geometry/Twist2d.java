package org.areslib.math.geometry;

import java.util.Objects;

/**
 * A change in distance along an arc since the last pose update. We can use ideas from differential calculus
 * to create new Pose2ds from a Twist2d and vice versa.
 *
 * <p>A Twist can be used to represent a difference between two poses.
 */
public class Twist2d {
    /** Linear "dx" component. */
    public double dx;
    /** Linear "dy" component. */
    public double dy;
    /** Angular "dtheta" component (radians). */
    public double dtheta;

    /**
     * Constructs a Twist2d with the given values.
     *
     * @param dx     Change in x direction relative to robot.
     * @param dy     Change in y direction relative to robot.
     * @param dtheta Change in angle (radians).
     */
    public Twist2d(double dx, double dy, double dtheta) {
        this.dx = dx;
        this.dy = dy;
        this.dtheta = dtheta;
    }

    /**
     * Constructs a Twist2d with zero values.
     */
    public Twist2d() {
        this(0.0, 0.0, 0.0);
    }

    /**
     * Scale this twist by a scalar.
     * @param scalar The scalar.
     * @return The scaled twist.
     */
    public Twist2d scaled(double scalar) {
        return new Twist2d(dx * scalar, dy * scalar, dtheta * scalar);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Twist2d twist2d = (Twist2d) obj;
        return Double.compare(twist2d.dx, dx) == 0 &&
               Double.compare(twist2d.dy, dy) == 0 &&
               Double.compare(twist2d.dtheta, dtheta) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dx, dy, dtheta);
    }
}

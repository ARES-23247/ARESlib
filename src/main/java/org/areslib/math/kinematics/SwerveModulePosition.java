package org.areslib.math.kinematics;

import org.areslib.math.geometry.Rotation2d;

/**
 * Represents the state of one swerve module relating to its distance and angle.
 */
public class SwerveModulePosition {
    public double distanceMeters;
    public Rotation2d angle;

    public SwerveModulePosition() {
        this.distanceMeters = 0.0;
        this.angle = new Rotation2d();
    }

    public SwerveModulePosition(double distanceMeters, Rotation2d angle) {
        this.distanceMeters = distanceMeters;
        this.angle = angle;
    }
}

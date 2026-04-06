package org.areslib.math.kinematics;

/**
 * Represents the wheel positions for a differential drive drivetrain.
 */
public class DifferentialDriveWheelPositions {
    public double leftMeters;
    public double rightMeters;

    public DifferentialDriveWheelPositions() {
    }

    public DifferentialDriveWheelPositions(double leftMeters, double rightMeters) {
        this.leftMeters = leftMeters;
        this.rightMeters = rightMeters;
    }
}

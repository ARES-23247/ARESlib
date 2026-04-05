package org.areslib.math.kinematics;

/**
 * Helper class that converts a chassis velocity (dx and dtheta components) into individual
 * wheel speeds for a differential drive (tank drive) robot.
 */
public class DifferentialDriveKinematics {
    /** The track width in meters representing the distance between the left and right wheels. */
    public final double trackWidthMeters;

    /**
     * Constructs a differential drive kinematics object.
     * @param trackWidthMeters The track width of the drivetrain.
     */
    public DifferentialDriveKinematics(double trackWidthMeters) {
        this.trackWidthMeters = trackWidthMeters;
    }

    /**
     * Converts a chassis speed to individual wheel speeds.
     * @param chassisSpeeds The chassis speeds.
     * @return The individual wheel speeds.
     */
    public DifferentialDriveWheelSpeeds toWheelSpeeds(ChassisSpeeds chassisSpeeds) {
        return new DifferentialDriveWheelSpeeds(
            chassisSpeeds.vxMetersPerSecond - trackWidthMeters / 2.0 * chassisSpeeds.omegaRadiansPerSecond,
            chassisSpeeds.vxMetersPerSecond + trackWidthMeters / 2.0 * chassisSpeeds.omegaRadiansPerSecond
        );
    }
}

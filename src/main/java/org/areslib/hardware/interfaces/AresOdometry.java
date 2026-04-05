package org.areslib.hardware.interfaces;

import org.areslib.math.geometry.Pose2d;

/**
 * Hardware-agnostic interface for a 2D generic odometry source.
 * Designed to wrap high-level external pose estimators like the 
 * GoBilda Pinpoint or SparkFun OTOS that compute position onboard.
 */
public interface AresOdometry {
    /**
     * Gets the computed position of the robot from the odometry system.
     * @return A Pose2d representing X meters, Y meters, and Heading.
     */
    Pose2d getPoseMeters();
}

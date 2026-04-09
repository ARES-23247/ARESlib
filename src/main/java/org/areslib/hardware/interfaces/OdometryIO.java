package org.areslib.hardware.interfaces;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.telemetry.AresLoggableInputs;

/**
 * AdvantageKit-style IO abstraction for Odometry systems (e.g., SparkFun OTOS, GoBilda Pinpoint, or
 * PathPlanner localizers).
 */
public interface OdometryIO {
  /** Loggable data object containing generic odometry state. */
  class OdometryInputs implements AresLoggableInputs {
    /** Estimated X position in meters. */
    public double xMeters = 0.0;

    /** Estimated Y position in meters. */
    public double yMeters = 0.0;

    /** Estimated heading in radians. */
    public double headingRadians = 0.0;

    /** Estimated X velocity in meters per second. */
    public double xVelocityMetersPerSecond = 0.0;

    /** Estimated Y velocity in meters per second. */
    public double yVelocityMetersPerSecond = 0.0;

    /** Estimated angular velocity in radians per second. */
    public double angularVelocityRadiansPerSecond = 0.0;

    /**
     * Returns the current estimated pose as a {@link Pose2d} object.
     *
     * @return The current estimated pose as a {@link Pose2d} object.
     */
    public Pose2d getPoseMeters() {
      return new Pose2d(xMeters, yMeters, new Rotation2d(headingRadians));
    }
  }

  /**
   * Updates the data structure with the latest values from the underlying hardware sensor or
   * simulation.
   *
   * @param inputs The OdometryInputs object to be populated.
   */
  default void updateInputs(OdometryInputs inputs) {}
}

package org.areslib.hardware.interfaces;

import org.areslib.telemetry.AresLoggableInputs;
import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;

public interface OdometryIO {
    class OdometryInputs implements AresLoggableInputs {
        public double xMeters = 0.0;
        public double yMeters = 0.0;
        public double headingRadians = 0.0;
        public double xVelocityMetersPerSecond = 0.0;
        public double yVelocityMetersPerSecond = 0.0;
        public double angularVelocityRadiansPerSecond = 0.0;

        public Pose2d getPoseMeters() {
            return new Pose2d(xMeters, yMeters, new Rotation2d(headingRadians));
        }
    }

    /** Updates the set of loggable inputs. */
    default void updateInputs(OdometryInputs inputs) {}
}

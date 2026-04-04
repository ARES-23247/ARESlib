package org.areslib.hardware;

import org.areslib.telemetry.AresLoggableInputs;

public interface MecanumDriveIO {
    class MecanumDriveInputs implements AresLoggableInputs {
        public double frontLeftPositionMeters = 0.0;
        public double frontLeftVelocityMps = 0.0;
        public double frontRightPositionMeters = 0.0;
        public double frontRightVelocityMps = 0.0;
        public double rearLeftPositionMeters = 0.0;
        public double rearLeftVelocityMps = 0.0;
        public double rearRightPositionMeters = 0.0;
        public double rearRightVelocityMps = 0.0;
    }

    /** Updates the set of loggable inputs. */
    default void updateInputs(MecanumDriveInputs inputs) {}

    /** Run the motors at the specified voltages. */
    default void setVoltages(double frontLeft, double frontRight, double rearLeft, double rearRight) {}
}

package org.areslib.hardware;

import org.areslib.telemetry.AresLoggableInputs;

public interface DifferentialDriveIO {
    class DifferentialDriveInputs implements AresLoggableInputs {
        public double leftPositionMeters = 0.0;
        public double leftVelocityMps = 0.0;
        public double rightPositionMeters = 0.0;
        public double rightVelocityMps = 0.0;
    }

    /** Updates the set of loggable inputs. */
    default void updateInputs(DifferentialDriveInputs inputs) {}

    /** Run the motors at the specified voltages. */
    default void setVoltages(double leftVolts, double rightVolts) {}
}

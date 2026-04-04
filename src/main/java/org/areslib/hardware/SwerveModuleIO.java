package org.areslib.hardware;

import org.areslib.telemetry.AresLoggableInputs;

public interface SwerveModuleIO {
    class SwerveModuleInputs implements AresLoggableInputs {
        public double drivePositionMeters = 0.0;
        public double driveVelocityMps = 0.0;
        public double turnAbsolutePositionRad = 0.0;
        public double turnVelocityRadPerSec = 0.0;
    }

    /** Updates the set of loggable inputs. */
    default void updateInputs(SwerveModuleInputs inputs) {}

    /** Run the drive motor at the specified voltage. */
    default void setDriveVoltage(double volts) {}

    /** Run the turn motor at the specified voltage. */
    default void setTurnVoltage(double volts) {}
}

package org.areslib.hardware;

import org.areslib.telemetry.AresLoggableInputs;

/**
 * AdvantageKit-style hardware abstraction interface for a single Swerve Module.
 * <p>
 * Defines the IO layer for a module containing one driving motor and one steering/turning motor
 * with absolute heading feedback.
 */
public interface SwerveModuleIO {
    
    /**
     * Loggable data object containing the state of a single swerve wheel pod.
     */
    class SwerveModuleInputs implements AresLoggableInputs {
        /** Integrated encoder position of the drive motor in meters. */
        public double drivePositionMeters = 0.0;
        /** Integrated encoder velocity of the drive motor in meters per second. */
        public double driveVelocityMps = 0.0;
        /** Absolute encoder position of the steering module in radians (0 to 2PI). */
        public double turnAbsolutePositionRad = 0.0;
        /** Angular velocity of the steering module in radians per second. */
        public double turnVelocityRadPerSec = 0.0;
    }

    /**
     * Retrieves the latest sensor data from the backend implementations (real or sim)
     * and packs them into the supplied {@link SwerveModuleInputs} object.
     *
     * @param inputs The input object reference to be populated with current state.
     */
    default void updateInputs(SwerveModuleInputs inputs) {}

    /**
     * Commands the drive motor using raw voltage.
     *
     * @param volts Target output voltage for the drive motor.
     */
    default void setDriveVoltage(double volts) {}

    /**
     * Commands the steering/turn motor using raw voltage.
     *
     * @param volts Target output voltage for the steering motor.
     */
    default void setTurnVoltage(double volts) {}
}

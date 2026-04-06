package org.areslib.subsystems.drive;

import org.areslib.telemetry.AresLoggableInputs;

/**
 * AdvantageKit-style hardware abstraction interface for a Differential (Tank) Drivetrain.
 * <p>
 * This interface defines how generic hardware IO interactions are handled for a tank drive system,
 * enabling hot-swapping between real physical hardware on a robot and a Dyn4j physics simulation.
 */
public interface DifferentialDriveIO {

    /**
     * Data object containing all inputs retrieved from the physical or simulated
     * differential drive motors. These values will be serialized via AdvKit telemetry.
     */
    class DifferentialDriveInputs implements AresLoggableInputs {
        /** Integrated encoder position of the left driveline in meters. */
        public double leftPositionMeters = 0.0;
        /** Integrated encoder velocity of the left driveline in meters per second. */
        public double leftVelocityMps = 0.0;
        /** Integrated encoder position of the right driveline in meters. */
        public double rightPositionMeters = 0.0;
        /** Integrated encoder velocity of the right driveline in meters per second. */
        public double rightVelocityMps = 0.0;
    }

    /**
     * Retrieves the latest sensor data from the backend implementations (real or sim)
     * and packs them into the supplied {@link DifferentialDriveInputs} object.
     *
     * @param inputs The input object reference to be populated with current state.
     */
    default void updateInputs(DifferentialDriveInputs inputs) {}

    /**
     * Commands the left and right sides of the differential drivetrain using raw voltage.
     *
     * @param leftVolts  Target output voltage for the left side (typically -12.0 to 12.0).
     * @param rightVolts Target output voltage for the right side (typically -12.0 to 12.0).
     */
    default void setVoltages(double leftVolts, double rightVolts) {}
}

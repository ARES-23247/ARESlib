package org.areslib.examples.elevator;

import org.areslib.telemetry.AresLoggableInputs;

public interface ElevatorIO {
    class ElevatorInputs implements AresLoggableInputs {
        public double positionMeters = 0.0;
        public double velocityMps = 0.0;
        public double appliedVolts = 0.0;
        public double currentAmps = 0.0;
    }

    /**
     * Updates the inputs with the latest sensor data.
     * @param inputs The inputs to update.
     */
    void updateInputs(ElevatorInputs inputs);

    /**
     * Set the voltage applied to the elevator motor.
     * @param volts The voltage (usually -12.0 to 12.0)
     */
    void setVoltage(double volts);

    /**
     * Set the target position of the grabber servo.
     * @param position Range 0.0 to 1.0.
     */
    void setGrabberServo(double position);

    /** Enable or disable current polling. */
    default void setCurrentPolling(boolean enabled) {}

    /** Reset the encoder. */
    default void resetEncoder() {}
}

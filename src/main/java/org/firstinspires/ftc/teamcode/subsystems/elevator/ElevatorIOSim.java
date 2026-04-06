package org.firstinspires.ftc.teamcode.subsystems.elevator;

/**
 * Simulated implementation of {@link ElevatorIO} for offline physics testing.
 * <p>
 * Models a vertical elevator with gravity. The gravity voltage constant should
 * match the {@code kG} feedforward used in the real subsystem so that PID gains
 * tuned in simulation transfer to hardware.
 */
public class ElevatorIOSim implements ElevatorIO {
    private static final double KV = 0.5; // meters per sec per volt
    /** Voltage required to hold against gravity — should match ElevatorConstants.kG */
    private static final double GRAVITY_VOLTS = 0.2;

    private double appliedVolts = 0.0;
    private double positionMeters = 0.0;
    private double velocityMps = 0.0;

    @Override
    public void updateInputs(ElevatorIOInputs inputs) {
        // Subtract gravity load from applied voltage before computing velocity
        double effectiveVolts = appliedVolts - GRAVITY_VOLTS;
        velocityMps = effectiveVolts * KV;
        positionMeters += velocityMps * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

        // Ensure floor bounds
        if (positionMeters < 0) {
            positionMeters = 0.0;
            velocityMps = 0.0;
        }

        // Output to inputs structure
        inputs.positionMeters = positionMeters;
        inputs.velocityMetersPerSec = velocityMps;
        inputs.appliedVolts = appliedVolts;
        inputs.currentAmps = new double[]{Math.abs(appliedVolts * 2.0)}; // Approximate current draw
    }

    @Override
    public void setVoltage(double volts) {
        appliedVolts = Math.max(-12.0, Math.min(12.0, volts));
    }
}


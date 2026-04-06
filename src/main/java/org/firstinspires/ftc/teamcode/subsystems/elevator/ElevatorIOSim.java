package org.firstinspires.ftc.teamcode.subsystems.elevator;

public class ElevatorIOSim implements ElevatorIO {
    private static final double KV = 0.5; // meters per sec per volt
    private static final double LOOP_PERIOD_SECS = 0.02; // 20ms

    private double appliedVolts = 0.0;
    private double positionMeters = 0.0;
    private double velocityMps = 0.0;

    @Override
    public void updateInputs(ElevatorIOInputs inputs) {
        // Integrate basic physics
        velocityMps = appliedVolts * KV;
        positionMeters += velocityMps * LOOP_PERIOD_SECS;

        // Ensure floor bounds
        if (positionMeters < 0) {
            positionMeters = 0.0;
            velocityMps = 0.0;
        }

        // Output to inputs structure
        inputs.positionMeters = positionMeters;
        inputs.velocityMetersPerSec = velocityMps;
        inputs.appliedVolts = appliedVolts;
        inputs.currentAmps = new double[]{Math.abs(appliedVolts * 2.0)}; // Fake current draw
    }

    @Override
    public void setVoltage(double volts) {
        appliedVolts = Math.max(-12.0, Math.min(12.0, volts));
    }
}

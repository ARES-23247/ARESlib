package org.areslib.subsystems.drive;

/**
 * Simulated implementation of {@link SwerveModuleIO} for offline physics testing.
 * <p>
 * This class uses a simple first-order kinematic model to approximate motor velocities
 * based on commanded voltages, integrating those velocities to track simulated positional state.
 * Heading wrap-around is automatically handled.
 */
public class SwerveModuleIOSim implements SwerveModuleIO {
    // Basic physics constants (Kv)
    private static final double DRIVE_KV = 0.4; // meters per sec per volt (1.0 / 2.5 FF)
    private static final double TURN_KV = 5.0; // radians per sec per volt
    private static final double LOOP_PERIOD_SECS = 0.02; // 20ms

    private double driveAppliedVolts = 0.0;
    private double turnAppliedVolts = 0.0;

    private double drivePositionMeters = 0.0;
    private double driveVelocityMps = 0.0;

    private double turnAbsolutePositionRad = 0.0;
    private double turnVelocityRadPerSec = 0.0;

    @Override
    public void updateInputs(SwerveModuleInputs inputs) {
        // Integrate basic physics
        driveVelocityMps = driveAppliedVolts * DRIVE_KV;
        drivePositionMeters += driveVelocityMps * LOOP_PERIOD_SECS;

        turnVelocityRadPerSec = turnAppliedVolts * TURN_KV;
        turnAbsolutePositionRad += turnVelocityRadPerSec * LOOP_PERIOD_SECS;

        // Wrap turn position to [-pi, pi]
        if (turnAbsolutePositionRad > Math.PI) {
            turnAbsolutePositionRad -= 2.0 * Math.PI;
        } else if (turnAbsolutePositionRad < -Math.PI) {
            turnAbsolutePositionRad += 2.0 * Math.PI;
        }

        // Output to inputs structure
        inputs.drivePositionMeters = drivePositionMeters;
        inputs.driveVelocityMps = driveVelocityMps;
        inputs.turnAbsolutePositionRad = turnAbsolutePositionRad;
        inputs.turnVelocityRadPerSec = turnVelocityRadPerSec;
    }

    @Override
    public void setDriveVoltage(double volts) {
        driveAppliedVolts = Math.max(-12.0, Math.min(12.0, volts));
    }

    @Override
    public void setTurnVoltage(double volts) {
        turnAppliedVolts = Math.max(-12.0, Math.min(12.0, volts));
    }
}

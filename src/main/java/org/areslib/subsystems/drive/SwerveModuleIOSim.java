package org.areslib.subsystems.drive;

import org.areslib.math.MathUtil;

/**
 * Simulated implementation of {@link SwerveModuleIO} for offline physics testing.
 * <p>
 * This class uses a simple first-order kinematic model to approximate motor velocities
 * based on commanded voltages, integrating those velocities to track simulated positional state.
 * Heading wrap-around is automatically handled via {@link MathUtil#angleModulus}.
 */
public class SwerveModuleIOSim implements SwerveModuleIO {
    // Basic physics constants (Kv)
    private static final double DRIVE_KV = 0.4; // meters per sec per volt (1.0 / 2.5 FF)
    private static final double TURN_KV = 5.0;     // rad/s per volt

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
        drivePositionMeters += driveVelocityMps * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

        turnVelocityRadPerSec = turnAppliedVolts * TURN_KV;
        turnAbsolutePositionRad += turnVelocityRadPerSec * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

        // Wrap turn position to [-pi, pi] — handles any magnitude, not just single overflow
        turnAbsolutePositionRad = MathUtil.angleModulus(turnAbsolutePositionRad);

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

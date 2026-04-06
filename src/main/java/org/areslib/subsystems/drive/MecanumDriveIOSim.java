package org.areslib.subsystems.drive;

/**
 * Simulated implementation of {@link MecanumDriveIO} for offline physics testing.
 * <p>
 * This class uses a simple first-order kinematic model to approximate motor velocities
 * based on commanded voltages, integrating those velocities to track simulated positional state.
 */
public class MecanumDriveIOSim implements MecanumDriveIO {
    private static final double DRIVE_KV = 3.0; // meters per sec per volt

    private double flAppliedVolts = 0.0;
    private double frAppliedVolts = 0.0;
    private double rlAppliedVolts = 0.0;
    private double rrAppliedVolts = 0.0;

    private double flPos = 0.0, flVel = 0.0;
    private double frPos = 0.0, frVel = 0.0;
    private double rlPos = 0.0, rlVel = 0.0;
    private double rrPos = 0.0, rrVel = 0.0;

    @Override
    public void updateInputs(MecanumDriveInputs inputs) {
        // Integrate physics
        flVel = flAppliedVolts * DRIVE_KV;
        flPos += flVel * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

        frVel = frAppliedVolts * DRIVE_KV;
        frPos += frVel * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

        rlVel = rlAppliedVolts * DRIVE_KV;
        rlPos += rlVel * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

        rrVel = rrAppliedVolts * DRIVE_KV;
        rrPos += rrVel * org.areslib.core.AresRobot.LOOP_PERIOD_SECS;

        // Populate inputs
        inputs.frontLeftPositionMeters = flPos;
        inputs.frontLeftVelocityMps = flVel;
        inputs.frontRightPositionMeters = frPos;
        inputs.frontRightVelocityMps = frVel;
        inputs.rearLeftPositionMeters = rlPos;
        inputs.rearLeftVelocityMps = rlVel;
        inputs.rearRightPositionMeters = rrPos;
        inputs.rearRightVelocityMps = rrVel;
    }

    @Override
    public void setVoltages(double frontLeft, double frontRight, double rearLeft, double rearRight) {
        flAppliedVolts = Math.max(-12.0, Math.min(12.0, frontLeft));
        frAppliedVolts = Math.max(-12.0, Math.min(12.0, frontRight));
        rlAppliedVolts = Math.max(-12.0, Math.min(12.0, rearLeft));
        rrAppliedVolts = Math.max(-12.0, Math.min(12.0, rearRight));
    }
}

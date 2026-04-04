package org.areslib.hardware;

import org.areslib.hardware.sensors.AresEncoder;
import org.areslib.hardware.interfaces.AresMotor;

public class MecanumDriveIOReal implements MecanumDriveIO {
    
    private final AresMotor flMotor, frMotor, rlMotor, rrMotor;
    private final AresEncoder flEncoder, frEncoder, rlEncoder, rrEncoder;
    private final double distancePerTick;

    public MecanumDriveIOReal(
            AresMotor flMotor, AresMotor frMotor, AresMotor rlMotor, AresMotor rrMotor,
            AresEncoder flEncoder, AresEncoder frEncoder, AresEncoder rlEncoder, AresEncoder rrEncoder,
            double distancePerTick) {
        this.flMotor = flMotor;
        this.frMotor = frMotor;
        this.rlMotor = rlMotor;
        this.rrMotor = rrMotor;
        this.flEncoder = flEncoder;
        this.frEncoder = frEncoder;
        this.rlEncoder = rlEncoder;
        this.rrEncoder = rrEncoder;
        this.distancePerTick = distancePerTick;
    }

    @Override
    public void updateInputs(MecanumDriveInputs inputs) {
        inputs.frontLeftPositionMeters = flEncoder.getPosition() * distancePerTick;
        inputs.frontLeftVelocityMps = flEncoder.getVelocity() * distancePerTick;

        inputs.frontRightPositionMeters = frEncoder.getPosition() * distancePerTick;
        inputs.frontRightVelocityMps = frEncoder.getVelocity() * distancePerTick;

        inputs.rearLeftPositionMeters = rlEncoder.getPosition() * distancePerTick;
        inputs.rearLeftVelocityMps = rlEncoder.getVelocity() * distancePerTick;

        inputs.rearRightPositionMeters = rrEncoder.getPosition() * distancePerTick;
        inputs.rearRightVelocityMps = rrEncoder.getVelocity() * distancePerTick;
    }

    @Override
    public void setVoltages(double frontLeft, double frontRight, double rearLeft, double rearRight) {
        flMotor.setVoltage(frontLeft);
        frMotor.setVoltage(frontRight);
        rlMotor.setVoltage(rearLeft);
        rrMotor.setVoltage(rearRight);
    }
}

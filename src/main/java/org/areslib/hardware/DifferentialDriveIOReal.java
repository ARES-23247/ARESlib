package org.areslib.hardware;

import org.areslib.hardware.sensors.AresEncoder;
import org.areslib.hardware.interfaces.AresMotor;

public class DifferentialDriveIOReal implements DifferentialDriveIO {

    private final AresMotor leftMotor;
    private final AresMotor rightMotor;
    
    private final AresEncoder leftEncoder;
    private final AresEncoder rightEncoder;
    
    private final double distancePerTick;

    public DifferentialDriveIOReal(
            AresMotor leftMotor, AresMotor rightMotor, 
            AresEncoder leftEncoder, AresEncoder rightEncoder, 
            double distancePerTick) {
        this.leftMotor = leftMotor;
        this.rightMotor = rightMotor;
        this.leftEncoder = leftEncoder;
        this.rightEncoder = rightEncoder;
        this.distancePerTick = distancePerTick;
    }

    @Override
    public void updateInputs(DifferentialDriveInputs inputs) {
        inputs.leftPositionMeters = leftEncoder.getPosition() * distancePerTick;
        inputs.leftVelocityMps = leftEncoder.getVelocity() * distancePerTick;

        inputs.rightPositionMeters = rightEncoder.getPosition() * distancePerTick;
        inputs.rightVelocityMps = rightEncoder.getVelocity() * distancePerTick;
    }

    @Override
    public void setVoltages(double leftVolts, double rightVolts) {
        leftMotor.setVoltage(leftVolts);
        rightMotor.setVoltage(rightVolts);
    }
}

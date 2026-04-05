package org.areslib.hardware;

import org.areslib.hardware.sensors.AresEncoder;
import org.areslib.hardware.sensors.AresAbsoluteEncoder;
import org.areslib.hardware.interfaces.AresMotor;

/**
 * Concrete implementation of {@link SwerveModuleIO} for physical robot hardware.
 * <p>
 * This class maps the logical swerve module commands to actual FTC motors and encoders.
 */
public class SwerveModuleIOReal implements SwerveModuleIO {
    
    private final AresMotor driveMotor;
    private final AresMotor turnMotor;
    private final AresEncoder driveEncoder;
    private final AresAbsoluteEncoder turnEncoder;

    /**
     * Constructs a physical Swerve Module IO layer.
     *
     * @param driveMotor   The hardware wrapper for the driving motor.
     * @param turnMotor    The hardware wrapper for the steering/turning motor.
     * @param driveEncoder The hardware wrapper for the driving encoder.
     * @param turnEncoder  The hardware wrapper for the absolute steering encoder.
     */
    public SwerveModuleIOReal(
            AresMotor driveMotor, 
            AresMotor turnMotor, 
            AresEncoder driveEncoder, 
            AresAbsoluteEncoder turnEncoder) {
        this.driveMotor = driveMotor;
        this.turnMotor = turnMotor;
        this.driveEncoder = driveEncoder;
        this.turnEncoder = turnEncoder;
    }

    @Override
    public void updateInputs(SwerveModuleInputs inputs) {
        // Direct, zero-latency reads from the abstracted cache. 
        // Handles transparent expansion hub bulk caching or native OctoQuad arrays equally.
        inputs.drivePositionMeters = driveEncoder.getPosition();
        inputs.driveVelocityMps = driveEncoder.getVelocity();
        
        inputs.turnAbsolutePositionRad = turnEncoder.getAbsolutePositionRad();
        inputs.turnVelocityRadPerSec = turnEncoder.getVelocity();
    }

    @Override
    public void setDriveVoltage(double volts) {
        driveMotor.setVoltage(volts);
    }

    @Override
    public void setTurnVoltage(double volts) {
        turnMotor.setVoltage(volts);
    }
}

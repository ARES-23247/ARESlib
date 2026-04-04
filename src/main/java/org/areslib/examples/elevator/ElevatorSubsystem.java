package org.areslib.examples.elevator;

import org.areslib.command.SubsystemBase;
import org.areslib.telemetry.AresAutoLogger;

public class ElevatorSubsystem extends SubsystemBase {

    public final ElevatorIO io;
    private final ElevatorIO.ElevatorInputs inputs = new ElevatorIO.ElevatorInputs();

    public ElevatorSubsystem(ElevatorIO io) {
        this.io = io;
    }

    @Override
    public void periodic() {
        // Update inputs every loop from cache or sim
        io.updateInputs(inputs);
        
        // Magically pushes all structure data to both Live WebSocket and .wpilog simultaneously.
        AresAutoLogger.processInputs("Elevator", inputs);
        
        // Note: Because of this architecture, adding a limit switch requires exactly two steps:
        // 1. Adding a boolean to ElevatorInputs.
        // 2. Reading it in ElevatorIOReal.
        // The AutoLogger dynamically handles ALL parsing, UI generation, and binary logging!
    }

    public void setVoltage(double volts) {
        io.setVoltage(volts);
    }
    
    public void closeGrabber() {
        io.setGrabberServo(1.0);
    }
    
    public void openGrabber() {
        io.setGrabberServo(0.0);
    }

    public double getPositionMeters() {
        return inputs.positionMeters;
    }

    public double getVelocityMps() {
        return inputs.velocityMps;
    }

    public ElevatorIO.ElevatorInputs getInputs() {
        return inputs;
    }

    public void resetEncoder() {
        io.resetEncoder();
    }
}

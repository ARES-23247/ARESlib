package org.firstinspires.ftc.teamcode.subsystems.elevator;

import org.areslib.command.Subsystem;
import org.areslib.telemetry.AresAutoLogger;

public class ElevatorSubsystem implements Subsystem {

    private final ElevatorIO io;
    private final ElevatorIO.ElevatorIOInputs inputs = new ElevatorIO.ElevatorIOInputs();

    // Standard PID gains for demonstration (teams should tune these)
    private static final double kP = 50.0;
    private static final double kG = 0.2; // Gravity feedforward
    
    private double targetPositionMeters = 0.0;

    public ElevatorSubsystem(ElevatorIO io) {
        this.io = io;
    }

    @Override
    public void periodic() {
        io.updateInputs(inputs);
        AresAutoLogger.processInputs("Elevator", inputs);

        // Compute error
        double error = targetPositionMeters - inputs.positionMeters;

        // Simple Proportional Control with Gravity Feedforward
        double volts = (error * kP) + kG;

        if (inputs.positionMeters >= 1.0 && volts > kG) { // Mock upper soft limit
             volts = kG;
        } else if (inputs.positionMeters <= 0.0 && volts < 0.0) { // Mock lower soft limit
             volts = 0.0;
        }

        io.setVoltage(volts);
        
        AresAutoLogger.recordOutput("Elevator/TargetPositionMeters", targetPositionMeters);
        AresAutoLogger.recordOutput("Elevator/ErrorMeters", error);
    }

    public void setTargetPosition(double positionMeters) {
        this.targetPositionMeters = Math.max(0.0, Math.min(positionMeters, 1.0)); // Clamp between 0 and 1 meter
    }

    public double getPositionMeters() {
        return inputs.positionMeters;
    }
    
    public double getVelocityMetersPerSec() {
        return inputs.velocityMetersPerSec;
    }
}

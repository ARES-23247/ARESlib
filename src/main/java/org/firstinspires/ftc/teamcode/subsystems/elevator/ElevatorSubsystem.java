package org.firstinspires.ftc.teamcode.subsystems.elevator;

import org.areslib.command.Subsystem;
import org.areslib.telemetry.AresAutoLogger;
import static org.firstinspires.ftc.teamcode.Constants.ElevatorConstants.*;

public class ElevatorSubsystem implements Subsystem {

    private final ElevatorIO io;
    private final ElevatorIO.ElevatorIOInputs inputs = new ElevatorIO.ElevatorIOInputs();

    // Standard PID gains for demonstration (teams should tune these)
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

        if (inputs.positionMeters >= MAX_POSITION_METERS && volts > kG) { // Mock upper soft limit
             volts = kG;
        } else if (inputs.positionMeters <= MIN_POSITION_METERS && volts < 0.0) { // Mock lower soft limit
             volts = 0.0;
        }

        io.setVoltage(volts);
        
        AresAutoLogger.recordOutput("Elevator/TargetPositionMeters", targetPositionMeters);
        AresAutoLogger.recordOutput("Elevator/ErrorMeters", error);
    }

    public void setTargetPosition(double positionMeters) {
        this.targetPositionMeters = Math.max(MIN_POSITION_METERS, Math.min(positionMeters, MAX_POSITION_METERS)); // Clamp
    }

    public double getPositionMeters() {
        return inputs.positionMeters;
    }
    
    public double getVelocityMetersPerSec() {
        return inputs.velocityMetersPerSec;
    }
}

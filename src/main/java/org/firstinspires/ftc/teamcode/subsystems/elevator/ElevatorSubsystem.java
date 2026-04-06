package org.firstinspires.ftc.teamcode.subsystems.elevator;

import org.areslib.command.SubsystemBase;
import org.areslib.telemetry.AresAutoLogger;
import static org.firstinspires.ftc.teamcode.Constants.ElevatorConstants.*;

public class ElevatorSubsystem extends SubsystemBase {

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

        if (inputs.positionMeters >= MAX_POSITION_METERS && volts > kG) {
             // At upper limit: only apply gravity hold, don't push higher
             volts = kG;
        } else if (inputs.positionMeters <= MIN_POSITION_METERS && error <= 0.0) {
             // At floor with no upward demand: zero output to prevent grinding into hard stop
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

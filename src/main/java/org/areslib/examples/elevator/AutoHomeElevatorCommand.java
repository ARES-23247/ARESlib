package org.areslib.examples.elevator;

import org.areslib.command.Command;

/**
 * Example command demonstrating how to automatically home an elevator using current sensing.
 * Moves the elevator down until a current spike indicates stalling against the physical hardstop.
 */
public class AutoHomeElevatorCommand extends Command {

    private final ElevatorSubsystem elevator;

    /**
     * Constructs an AutoHomeElevatorCommand.
     * @param elevator The elevator subsystem to home.
     */
    public AutoHomeElevatorCommand(ElevatorSubsystem elevator) {
        this.elevator = elevator;
        addRequirements(elevator);
    }

    @Override
    public void initialize() {
        // Safely wake up the RS-485 current sensor
        elevator.io.setCurrentPolling(true);
    }

    @Override
    public void execute() {
        // Command the elevator to move downwards slowly
        elevator.setVoltage(-2.0);
    }

    @Override
    public boolean isFinished() {
        // Check for a current spike indicating a motor stall against the bottom limit
        return elevator.getInputs().currentAmps > 5.0;
    }

    @Override
    public void end(boolean interrupted) {
        // Immediately stop the motor
        elevator.setVoltage(0.0);
        
        // If it successfully stalled against the limit and finished
        if (!interrupted) {
            elevator.resetEncoder();
        }

        // CRITICAL: Always put the sensor back to sleep to restore our sub-10ms match loop times
        elevator.io.setCurrentPolling(false);
    }
}

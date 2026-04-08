package org.firstinspires.ftc.teamcode.commands;

import org.areslib.command.Command;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorSubsystem;

public class ElevatorToPositionCommand extends Command {
  /** Position tolerance in meters — command finishes when within this distance of target. */
  private static final double POSITION_TOLERANCE_METERS = 0.05;

  private final ElevatorSubsystem elevator;
  private final double targetPositionMeters;

  public ElevatorToPositionCommand(ElevatorSubsystem elevator, double targetPositionMeters) {
    this.elevator = elevator;
    this.targetPositionMeters = targetPositionMeters;
    addRequirements(elevator);
  }

  @Override
  public void initialize() {
    elevator.setTargetPosition(targetPositionMeters);
  }

  @Override
  public void execute() {
    // Continuous logic would go here if position was interpolated mapping
  }

  @Override
  public boolean isFinished() {
    return Math.abs(elevator.getPositionMeters() - targetPositionMeters)
        < POSITION_TOLERANCE_METERS;
  }

  @Override
  public void end(boolean interrupted) {
    // Optional logic when reaching target or being overridden
  }
}

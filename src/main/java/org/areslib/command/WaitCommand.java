package org.areslib.command;

/** A command that does nothing but takes a specified amount of time to finish. */
public class WaitCommand extends Command {
  private final double durationSeconds;
  private double startTimeSeconds;

  /**
   * Creates a new WaitCommand.
   *
   * @param seconds the time to wait, in seconds
   */
  public WaitCommand(double seconds) {
    durationSeconds = seconds;
  }

  @Override
  public void initialize() {
    startTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();
  }

  @Override
  public void execute() {
    // No operation required since we use absolute time difference
  }

  @Override
  public boolean isFinished() {
    return org.areslib.core.AresTimer.getFPGATimestamp() - startTimeSeconds >= durationSeconds;
  }
}

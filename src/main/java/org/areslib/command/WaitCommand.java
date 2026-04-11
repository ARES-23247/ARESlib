package org.areslib.command;

/** A command that does nothing but takes a specified amount of time to finish. */
public class WaitCommand extends Command {
  private final double durationSeconds;
  private double elapsedSeconds;

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
    elapsedSeconds = 0.0;
  }

  @Override
  public void execute() {
    elapsedSeconds +=
        org.areslib.core.AresRobot.LOOP_PERIOD_SECS; // deterministic 50Hz base loop period
  }

  @Override
  public boolean isFinished() {
    return elapsedSeconds >= durationSeconds;
  }
}

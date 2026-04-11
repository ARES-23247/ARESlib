package org.areslib.command;

/** A Command that runs instantly; it will initialize, execute once, and finish immediately. */
public class InstantCommand extends Command {
  private final Runnable toRun;

  /**
   * Creates a new InstantCommand that runs the given Runnable.
   *
   * @param toRun the Runnable to run
   * @param requirements the subsystems required by this command
   */
  public InstantCommand(Runnable toRun, Subsystem... requirements) {
    this.toRun = toRun;
    addRequirements(requirements);
  }

  /**
   * Creates a new InstantCommand with a Runnable that does nothing. Useful only as a no-arg
   * WaitCommand.
   */
  public InstantCommand() {
    toRun = () -> {};
  }

  @Override
  public void initialize() {
    toRun.run();
  }

  @Override
  public boolean isFinished() {
    return true;
  }
}

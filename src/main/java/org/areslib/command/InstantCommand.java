package org.areslib.command;

/** A Command that runs instantly; it will initialize, execute once, and finish immediately. */
public class InstantCommand extends Command {
  private final Runnable m_toRun;

  /**
   * Creates a new InstantCommand that runs the given Runnable.
   *
   * @param toRun the Runnable to run
   * @param requirements the subsystems required by this command
   */
  public InstantCommand(Runnable toRun, Subsystem... requirements) {
    m_toRun = toRun;
    addRequirements(requirements);
  }

  /**
   * Creates a new InstantCommand with a Runnable that does nothing. Useful only as a no-arg
   * WaitCommand.
   */
  public InstantCommand() {
    m_toRun = () -> {};
  }

  @Override
  public void initialize() {
    m_toRun.run();
  }

  @Override
  public boolean isFinished() {
    return true;
  }
}

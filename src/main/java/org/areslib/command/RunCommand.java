package org.areslib.command;

/**
 * A command that runs a Runnable continuously. Has no end condition as it is
 * intended to be interrupted.
 */
public class RunCommand extends Command {
    private final Runnable m_toRun;

    /**
     * Creates a new RunCommand. The Runnable will be run continuously until the
     * command is interrupted.
     *
     * @param toRun        the Runnable to run
     * @param requirements the subsystems required by this command
     */
    public RunCommand(Runnable toRun, Subsystem... requirements) {
        m_toRun = toRun;
        addRequirements(requirements);
    }

    @Override
    public void execute() {
        m_toRun.run();
    }
}

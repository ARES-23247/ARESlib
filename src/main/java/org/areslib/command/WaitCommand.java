package org.areslib.command;

/**
 * A command that does nothing but takes a specified amount of time to finish.
 */
public class WaitCommand extends Command {
    private final double m_durationSeconds;
    private double m_elapsedSeconds;

    /**
     * Creates a new WaitCommand.
     *
     * @param seconds the time to wait, in seconds
     */
    public WaitCommand(double seconds) {
        m_durationSeconds = seconds;
    }

    @Override
    public void initialize() {
        m_elapsedSeconds = 0.0;
    }

    @Override
    public void execute() {
        m_elapsedSeconds += 0.02; // deterministic 50Hz base loop period
    }

    @Override
    public boolean isFinished() {
        return m_elapsedSeconds >= m_durationSeconds;
    }
}

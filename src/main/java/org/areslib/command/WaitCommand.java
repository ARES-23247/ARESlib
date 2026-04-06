package org.areslib.command;

/**
 * A command that does nothing but takes a specified amount of time to finish.
 */
public class WaitCommand extends Command {
    private final double m_durationSeconds;
    private long m_startTimeMillis;

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
        m_startTimeMillis = System.currentTimeMillis();
    }

    @Override
    public boolean isFinished() {
        return (System.currentTimeMillis() - m_startTimeMillis) >= (m_durationSeconds * 1000.0);
    }
}

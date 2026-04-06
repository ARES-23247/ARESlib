package org.areslib.math.filter;

/**
 * A simple boolean filter that prevents repetitive, noisy triggers.
 * It requires a boolean signal to be steady for a specified time before changing state.
 */
public class Debouncer {
    public enum DebounceType {
        kRising,
        kFalling,
        kBoth
    }

    private final double m_debounceTimeSeconds;
    private final DebounceType m_debounceType;
    private boolean m_baseline;
    private double m_elapsedTimeSeconds;

    /**
     * Creates a new Debouncer.
     *
     * @param debounceTime The number of seconds the value must change from baseline for to be valid.
     * @param type         Which type of state change the debouncing will be performed on.
     */
    public Debouncer(double debounceTime, DebounceType type) {
        m_debounceTimeSeconds = debounceTime;
        m_debounceType = type;
        m_baseline = false;
        m_elapsedTimeSeconds = 0.0;
    }

    /**
     * Creates a new Debouncer with a default 'kRising' debounce type.
     *
     * @param debounceTime The number of seconds the value must change from baseline for to be valid.
     */
    public Debouncer(double debounceTime) {
        this(debounceTime, DebounceType.kRising);
    }

    private void resetTimer() {
        m_elapsedTimeSeconds = 0.0;
    }

    private boolean hasElapsed() {
        return m_elapsedTimeSeconds >= m_debounceTimeSeconds;
    }

    /**
     * Applies the debouncer to the input stream deterministically.
     *
     * @param input The current value of the input stream.
     * @param periodSeconds Time elapsed since the last method call.
     * @return The debounced value of the input stream.
     */
    public boolean calculate(boolean input, double periodSeconds) {
        if (input != m_baseline) {
            m_elapsedTimeSeconds += periodSeconds;
        }
        if (input == m_baseline) {
            resetTimer();
        }

        if (hasElapsed()) {
            if (m_debounceType == DebounceType.kBoth ||
               (m_debounceType == DebounceType.kRising && input) ||
               (m_debounceType == DebounceType.kFalling && !input)) {
                m_baseline = input;
                return m_baseline;
            }
        }

        return m_baseline;
    }
}

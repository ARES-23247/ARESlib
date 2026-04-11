package org.areslib.math.filter;

/**
 * A simple boolean filter that prevents repetitive, noisy triggers. It requires a boolean signal to
 * be steady for a specified time before changing state.
 */
public class Debouncer {
  public enum DebounceType {
    kRising,
    kFalling,
    kBoth
  }

  private final double debounceTimeSeconds;
  private final DebounceType debounceType;
  private boolean baseline;
  private double elapsedTimeSeconds;

  /**
   * Creates a new Debouncer.
   *
   * @param debounceTime The number of seconds the value must change from baseline for to be valid.
   * @param type Which type of state change the debouncing will be performed on.
   */
  public Debouncer(double debounceTime, DebounceType type) {
    debounceTimeSeconds = debounceTime;
    debounceType = type;
    baseline = false;
    elapsedTimeSeconds = 0.0;
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
    elapsedTimeSeconds = 0.0;
  }

  private boolean hasElapsed() {
    return elapsedTimeSeconds >= debounceTimeSeconds;
  }

  /**
   * Applies the debouncer to the input stream deterministically.
   *
   * @param input The current value of the input stream.
   * @param periodSeconds Time elapsed since the last method call.
   * @return The debounced value of the input stream.
   */
  public boolean calculate(boolean input, double periodSeconds) {
    if (input != baseline) {
      elapsedTimeSeconds += periodSeconds;
    }
    if (input == baseline) {
      resetTimer();
    }

    if (hasElapsed()) {
      if (debounceType == DebounceType.kBoth
          || (debounceType == DebounceType.kRising && input)
          || (debounceType == DebounceType.kFalling && !input)) {
        baseline = input;
        return baseline;
      }
    }

    return baseline;
  }
}

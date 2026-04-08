package org.areslib.core;

import org.areslib.telemetry.AresAutoLogger;

/**
 * A runtime-tunable number backed by FTC Dashboard.
 *
 * <p>Wraps a {@code double} value that can be changed live from the FTC Dashboard web interface
 * without redeploying code. The value is published to telemetry on creation and on each change,
 * allowing live monitoring and adjustment.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * TunableNumber kP = new TunableNumber("Drive/kP", 0.1);
 * TunableNumber kD = new TunableNumber("Drive/kD", 0.01);
 *
 * // In periodic():
 * if (kP.hasChanged()) {
 *     drivePid.setP(kP.get());
 * }
 * }</pre>
 */
public class TunableNumber {

  private final String m_key;
  private double m_value;
  private double m_lastValue;

  /**
   * Constructs a TunableNumber with a dashboard key and default value.
   *
   * @param key The telemetry key used for display and editing (e.g., "Drive/kP").
   * @param defaultValue The initial value.
   */
  public TunableNumber(String key, double defaultValue) {
    m_key = key;
    m_value = defaultValue;
    m_lastValue = defaultValue;

    // Publish the initial value
    AresAutoLogger.recordOutput("Tunables/" + m_key, m_value);
  }

  /**
   * Gets the current value.
   *
   * @return The current tunable value.
   */
  public double get() {
    return m_value;
  }

  /**
   * Sets the value programmatically and publishes it.
   *
   * @param value The new value.
   */
  public void set(double value) {
    m_value = value;
    AresAutoLogger.recordOutput("Tunables/" + m_key, value);
  }

  /**
   * Returns true if the value has changed since the last call to {@code hasChanged()}. Use this to
   * avoid reconfiguring controllers every loop when the value hasn't changed.
   *
   * @return True if the value changed.
   */
  public boolean hasChanged() {
    double current = m_value;
    if (Math.abs(current - m_lastValue) > 1e-12) {
      m_lastValue = current;
      return true;
    }
    return false;
  }

  /**
   * Returns the dashboard key for this tunable number.
   *
   * @return The key.
   */
  public String getKey() {
    return m_key;
  }

  @Override
  public String toString() {
    return String.format("TunableNumber(%s=%.4f)", m_key, m_value);
  }
}

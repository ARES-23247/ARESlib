package org.areslib.core;

import java.util.HashMap;
import java.util.Map;
import org.areslib.telemetry.AresAutoLogger;

/**
 * A runtime-tunable number backed by FTC Dashboard for live PID tuning.
 *
 * <p>Wraps a {@code double} value that can be changed live from the FTC Dashboard web interface
 * without redeploying code. The value is published to telemetry on creation and on each change,
 * allowing live monitoring and adjustment.
 *
 * <p><b>Per-Consumer Change Tracking:</b> Multiple consumers (e.g., a PID controller and a
 * feedforward model) can independently track whether they've seen the latest value using {@link
 * #hasChanged(int)}. Each consumer is identified by a unique integer ID.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * TunableNumber kP = new TunableNumber("Drive/kP", 0.1);
 * TunableNumber kD = new TunableNumber("Drive/kD", 0.01);
 *
 * // In periodic():
 * if (kP.hasChanged(hashCode())) {
 *     drivePid.setP(kP.get());
 * }
 * }</pre>
 */
public class TunableNumber {

  private final String m_key;
  private double m_value;
  private double m_defaultValue;

  /** Per-consumer change tracking: maps consumer ID → last seen value. */
  private final Map<Integer, Double> m_lastHasChangedValues = new HashMap<>();

  /**
   * Constructs a TunableNumber with a dashboard key and default value.
   *
   * @param key The telemetry key used for display and editing (e.g., "Drive/kP").
   * @param defaultValue The initial value.
   */
  public TunableNumber(String key, double defaultValue) {
    m_key = key;
    m_value = defaultValue;
    m_defaultValue = defaultValue;

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
   * Returns true if the value has changed since the last time this specific consumer checked. Each
   * consumer is tracked independently by the provided ID.
   *
   * <p><b>Students:</b> Use {@code this.hashCode()} or a constant int as the consumer ID. This
   * allows multiple subsystems to independently detect tunable changes without interfering with
   * each other.
   *
   * @param id A unique identifier for the consumer (e.g., {@code this.hashCode()}).
   * @return Whether the current value differs from the last value seen by this consumer.
   */
  public boolean hasChanged(int id) {
    double currentValue = m_value;
    Double lastValue = m_lastHasChangedValues.get(id);
    if (lastValue == null || Math.abs(currentValue - lastValue) > 1e-12) {
      m_lastHasChangedValues.put(id, currentValue);
      return true;
    }
    return false;
  }

  /**
   * Returns the default value this tunable was constructed with.
   *
   * @return The default value.
   */
  public double getDefault() {
    return m_defaultValue;
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

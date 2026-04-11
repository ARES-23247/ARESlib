package org.areslib.faults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.areslib.hardware.wrappers.AresGamepad;
import org.areslib.telemetry.AresTelemetry;

/**
 * Manages all active ALERTS and hardware faults, dispatching them to telemetry and providing
 * haptic/visual feedback to the driver station.
 *
 * <p><b>Architecture Note -- Two-Layer Fault System:</b>
 *
 * <pre>
 *   +--------------------------------------------------------------+
 *   |  Layer 1: IO-Level Health Monitoring                         |
 *   |  Package: org.areslib.hardware.faults                        |
 *   |  Classes: FaultMonitor (interface), RobotHealthTracker       |
 *   |  Role:    Polls hardware IO classes every loop tick.          |
 *   |           Detects disconnected sensors / I2C failures.        |
 *   |           Blasts to AdvantageScope "System/ActiveFaults".     |
 *   |           Bridges detected faults UP to this layer (below).   |
 *   +-------------------------|------------------------------------+
 *                              | Creates/sets AresAlert objects
 *   +-------------------------v------------------------------------+
 *   |  Layer 2: User-Facing Alert System                           |
 *   |  Package: org.areslib.faults (this package)                  |
 *   |  Classes: AresAlert, AresFaultManager, AresDiagnostics       |
 *   |  Role:    Manages alert lifecycle (active/inactive).          |
 *   |           Drives gamepad rumble + LED color feedback.          |
 *   |           Publishes categorized ALERTS to telemetry.           |
 *   |           AresDiagnostics runs pre-match hardware scans.      |
 *   +--------------------------------------------------------------+
 * </pre>
 *
 * A hardware fault detected at Layer 1 automatically becomes an active {@link AresAlert} at Layer
 * 2, triggering both AdvantageScope logging AND gamepad driver feedback.
 */
public class AresFaultManager {
  private static final List<AresAlert> ALERTS = new CopyOnWriteArrayList<>();
  private static AresGamepad driverGamepad;

  // Pre-allocated lists to eliminate per-tick GC pressure in update()
  private static final List<String> CACHED_ERRORS = new ArrayList<>();
  private static final List<String> CACHED_WARNINGS = new ArrayList<>();
  private static final List<String> CACHED_INFOS = new ArrayList<>();

  private static final Map<Integer, String[]> ARRAY_POOL = new HashMap<>();

  private static boolean wasError = false;
  private static boolean hasNewError = false;

  /**
   * Initializes the Fault Manager with the driver gamepad to allow rumble and LED feedback.
   *
   * @param gamepad The primary driver gamepad wrapper.
   */
  public static void initialize(AresGamepad gamepad) {
    driverGamepad = gamepad;
    wasError = false;
  }

  /**
   * Resets the manager, clearing the gamepad reference and the active ALERTS list.
   *
   * <p><b>Important:</b> Static {@link AresAlert} fields (e.g., declared as constants in subsystem
   * classes or {@link org.areslib.hardware.AresHardwareManager}) will re-register themselves in
   * {@code ALERTS} the next time {@link AresAlert#set(boolean)} is called with {@code true}. This
   * is intentional — persistent hardware fault alerts survive OpMode transitions so they can
   * re-announce if the underlying fault condition is still active.
   */
  public static void reset() {
    driverGamepad = null;
    wasError = false;
    hasNewError = false;
    ALERTS.clear();
  }

  /**
   * Returns true if a new error was detected on the last loop. Clears the flag upon reading to
   * allow edge-detection pulsing.
   */
  public static boolean hasNewError() {
    boolean val = hasNewError;
    hasNewError = false;
    return val;
  }

  /**
   * Registers an alert with the system. Handled automatically when AresAlert is constructed.
   *
   * @param alert The alert to register.
   */
  public static void registerAlert(AresAlert alert) {
    if (!ALERTS.contains(alert)) {
      ALERTS.add(alert);
    }
  }

  /** Periodically updates telemetry arrays and driver feedback. Must be called in the main loop. */
  public static void update() {
    CACHED_ERRORS.clear();
    CACHED_WARNINGS.clear();
    CACHED_INFOS.clear();

    boolean hasError = false;

    for (AresAlert alert : ALERTS) {
      if (alert.isActive()) {
        switch (alert.getType()) {
          case ERROR:
            CACHED_ERRORS.add(alert.getText());
            hasError = true;
            break;
          case WARNING:
            CACHED_WARNINGS.add(alert.getText());
            break;
          case INFO:
            CACHED_INFOS.add(alert.getText());
            break;
        }
      }
    }

    String[] errArray = ARRAY_POOL.computeIfAbsent(CACHED_ERRORS.size(), String[]::new);
    String[] warnArray = ARRAY_POOL.computeIfAbsent(CACHED_WARNINGS.size(), String[]::new);
    String[] infoArray = ARRAY_POOL.computeIfAbsent(CACHED_INFOS.size(), String[]::new);

    AresTelemetry.putStringArray("Alerts/Errors", CACHED_ERRORS.toArray(errArray));
    AresTelemetry.putStringArray("Alerts/Warnings", CACHED_WARNINGS.toArray(warnArray));
    AresTelemetry.putStringArray("Alerts/Infos", CACHED_INFOS.toArray(infoArray));

    // Edge detection for error state transitions — works even without a gamepad
    if (hasError && !wasError) {
      hasNewError = true;
    }

    if (driverGamepad != null) {
      if (hasError) {
        // If there's an active error, turn the controller red
        driverGamepad.setLedColor(1.0, 0.0, 0.0, 100);
      } else if (wasError) {
        // Issue resolved, reset to green
        driverGamepad.setLedColor(0.0, 1.0, 0.0, 1000);
      }
    }

    wasError = hasError;
  }
}

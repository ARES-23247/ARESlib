package org.areslib.faults;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.areslib.hardware.wrappers.AresGamepad;
import org.areslib.telemetry.AresTelemetry;

/**
 * Manages all active alerts and hardware faults, dispatching them to telemetry and providing
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
 *   |           Publishes categorized alerts to telemetry.           |
 *   |           AresDiagnostics runs pre-match hardware scans.      |
 *   +--------------------------------------------------------------+
 * </pre>
 *
 * A hardware fault detected at Layer 1 automatically becomes an active {@link AresAlert} at Layer
 * 2, triggering both AdvantageScope logging AND gamepad driver feedback.
 */
public class AresFaultManager {
  private static final List<AresAlert> alerts = new CopyOnWriteArrayList<>();
  private static AresGamepad driverGamepad;

  private static boolean wasError = false;

  /**
   * Initializes the Fault Manager with the driver gamepad to allow rumble and LED feedback.
   *
   * @param gamepad The primary driver gamepad wrapper.
   */
  public static void initialize(AresGamepad gamepad) {
    driverGamepad = gamepad;
    wasError = false;
  }

  /** Resets the manager, clearing the gamepad reference but keeping registered alerts. */
  public static void reset() {
    driverGamepad = null;
    wasError = false;
    alerts.clear();
  }

  /**
   * Registers an alert with the system. Handled automatically when AresAlert is constructed.
   *
   * @param alert The alert to register.
   */
  public static void registerAlert(AresAlert alert) {
    if (!alerts.contains(alert)) {
      alerts.add(alert);
    }
  }

  /** Periodically updates telemetry arrays and driver feedback. Must be called in the main loop. */
  public static void update() {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    List<String> infos = new ArrayList<>();

    boolean hasError = false;

    for (AresAlert alert : alerts) {
      if (alert.isActive()) {
        switch (alert.getType()) {
          case ERROR:
            errors.add(alert.getText());
            hasError = true;
            break;
          case WARNING:
            warnings.add(alert.getText());
            break;
          case INFO:
            infos.add(alert.getText());
            break;
        }
      }
    }

    AresTelemetry.putStringArray("Alerts/Errors", errors.toArray(new String[0]));
    AresTelemetry.putStringArray("Alerts/Warnings", warnings.toArray(new String[0]));
    AresTelemetry.putStringArray("Alerts/Infos", infos.toArray(new String[0]));

    if (driverGamepad != null) {
      if (hasError) {
        // If there's an active error, turn the controller red
        driverGamepad.setLedColor(1.0, 0.0, 0.0, 100);

        // Trigger a rumble the moment a new error appears
        if (!wasError) {
          driverGamepad.rumble(500);
        }
      } else if (wasError) {
        // Issue resolved, reset to green
        driverGamepad.setLedColor(0.0, 1.0, 0.0, 1000);
      }
    }

    wasError = hasError;
  }
}

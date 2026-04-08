package org.areslib.hardware.faults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.areslib.command.SubsystemBase;
import org.areslib.faults.AresAlert;
import org.areslib.telemetry.AresAutoLogger;

/**
 * Global singleton subsystem that continuously checks for hardware faults across all registered
 * {@link FaultMonitor} devices and blasts them to both AdvantageScope telemetry AND the {@link
 * org.areslib.faults.AresFaultManager} alert system (which provides gamepad rumble and LED
 * feedback).
 *
 * <p><b>Must be registered with the CommandScheduler</b> for its periodic() to run. This is handled
 * automatically by {@link org.areslib.core.AresCommandOpMode}.
 */
public class RobotHealthTracker extends SubsystemBase {

  private static RobotHealthTracker instance;
  private final List<FaultMonitor> monitors = new ArrayList<>();

  /** Lazily-created AresAlerts bridged to AresFaultManager for each monitor. */
  private final Map<FaultMonitor, AresAlert> alertBridge = new HashMap<>();

  private RobotHealthTracker() {
    // Singleton pattern
  }

  /**
   * Returns the singleton instance (thread-safe).
   *
   * @return The global RobotHealthTracker.
   */
  public static synchronized RobotHealthTracker getInstance() {
    if (instance == null) {
      instance = new RobotHealthTracker();
    }
    return instance;
  }

  /**
   * Resets the singleton. Must be called during OpMode transitions to prevent stale monitors from
   * previous OpModes.
   */
  public static synchronized void reset() {
    if (instance != null) {
      instance.monitors.clear();
      instance.alertBridge.clear();
    }
    instance = null;
  }

  /**
   * Registers a new fault monitor to be checked every robot loop.
   *
   * @param monitor The hardware component implementing FaultMonitor.
   */
  public void registerMonitor(FaultMonitor monitor) {
    if (!monitors.contains(monitor)) {
      monitors.add(monitor);
    }
  }

  /**
   * Returns true if any registered monitor currently reports a hardware fault.
   *
   * @return True if at least one fault is active.
   */
  public boolean hasActiveFaults() {
    for (FaultMonitor monitor : monitors) {
      if (monitor.hasHardwareFault()) return true;
    }
    return false;
  }

  @Override
  public void periodic() {
    List<String> activeFaults = new ArrayList<>();

    for (FaultMonitor monitor : monitors) {
      // Lazily create an AresAlert for each monitor so AresFaultManager picks it up
      AresAlert alert =
          alertBridge.computeIfAbsent(
              monitor, m -> new AresAlert(m.getFaultMessage(), AresAlert.AlertType.ERROR));

      if (monitor.hasHardwareFault()) {
        activeFaults.add(monitor.getFaultMessage());
        alert.setText(monitor.getFaultMessage());
        alert.set(true);
        System.err.println("[ERR] HARDWARE FAULT DETECTED: " + monitor.getFaultMessage());
      } else {
        alert.set(false);
      }
    }

    // Blast to AdvantageScope Console and StringArray variables
    if (!activeFaults.isEmpty()) {
      AresAutoLogger.recordOutput("System/ActiveFaults", activeFaults.toArray(new String[0]));
      AresAutoLogger.recordOutput(
          "Console", "[ERR] " + activeFaults.size() + " Hardware Fault(s) Active!");
    } else {
      AresAutoLogger.recordOutput("System/ActiveFaults", new String[] {"OK"});
    }
  }
}

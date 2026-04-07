package org.areslib.hardware.faults;

import org.areslib.command.SubsystemBase;
import org.areslib.telemetry.AresAutoLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Global singleton subsystem that continuously checks for hardware faults
 * across all registered {@link FaultMonitor} devices and blasts them to AdvantageScope.
 */
public class RobotHealthTracker extends SubsystemBase {
    
    private static RobotHealthTracker instance;
    private final List<FaultMonitor> monitors = new ArrayList<>();

    private RobotHealthTracker() {
        // Singleton pattern
    }

    public static RobotHealthTracker getInstance() {
        if (instance == null) {
            instance = new RobotHealthTracker();
        }
        return instance;
    }

    /**
     * Registers a new fault monitor to be checked every robot loop.
     * @param monitor The hardware component implementing FaultMonitor.
     */
    public void registerMonitor(FaultMonitor monitor) {
        if (!monitors.contains(monitor)) {
            monitors.add(monitor);
        }
    }

    @Override
    public void periodic() {
        List<String> activeFaults = new ArrayList<>();

        for (FaultMonitor monitor : monitors) {
            if (monitor.hasHardwareFault()) {
                activeFaults.add(monitor.getFaultMessage());
                // Blast loud alert to console
                System.err.println("[ERR] HARDWARE FAULT DETECTED: " + monitor.getFaultMessage());
            }
        }

        // Blast to AdvantageScope Console and StringArray variables
        if (!activeFaults.isEmpty()) {
            AresAutoLogger.recordOutput("System/ActiveFaults", activeFaults.toArray(new String[0]));
            // In AdvantageScope, pushing a string with [ERR] can natively trigger red highlights in the Console tab
            AresAutoLogger.recordOutput("Console", "[ERR] " + activeFaults.size() + " Hardware Fault(s) Active!");
        } else {
            AresAutoLogger.recordOutput("System/ActiveFaults", new String[]{"OK"});
        }
    }
}

package org.areslib.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.areslib.hardware.coprocessors.AresOctoQuadDriver;
import org.areslib.hardware.coprocessors.OctoQuadV1Impl;
import org.areslib.hardware.coprocessors.OctoQuadV2Impl;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.hardware.AnalogInput;
import org.areslib.telemetry.AresLoggableInputs;
import org.areslib.telemetry.AresAutoLogger;

/**
 * Global manager for physical and simulated hardware environments in ARESLib.
 * <p>
 * This class abstracts the initialization and persistent state of advanced FTC coprocessors
 * such as the OctoQuad (V1/V2) and custom SRS Hubs. It also manages system-level power 
 * inputs like battery voltage and floodgate (current) sensors, computing a safe dynamic
 * power scaling factor to prevent robot brownouts.
 */
public class AresHardwareManager {

    private static org.areslib.hardware.coprocessors.AresOctoQuadDriver activeOctoQuad;
    private static org.areslib.hardware.coprocessors.SRSHub activeSrsHub;
    private static VoltageSensor batteryVoltageSensor;
    private static org.areslib.hardware.interfaces.AresAnalogSensor floodgateSensor;

    /** The last measured system battery voltage. */
    public static double batteryVoltage = 12.0;
    /** The last measured total chassis current in Amps. */
    public static double totalCurrentAmps = 0.0;
    /** The computed master power multiplier (0.0 to 1.0) applied to chassis actuators. */
    public static double masterPowerScale = 1.0;

    /** Loggable structured representation of the global power status. */
    public static class AresPowerInputs implements AresLoggableInputs {
        public double batteryVoltage = 12.0;
        public double totalCurrentAmps = 0.0;
        public double masterPowerScale = 1.0;
    }
    private static final AresPowerInputs powerInputs = new AresPowerInputs();

    /**
     * Retrieves the currently active SRS Coprocessor Hub, if configured.
     *
     * @return The active {@link org.areslib.hardware.coprocessors.SRSHub}, or null if absent.
     */
    public static org.areslib.hardware.coprocessors.SRSHub getActiveSrsHub() {
        return activeSrsHub;
    }

    /**
     * Retrieves the currently active OctoQuad Encoder interface, if configured.
     *
     * @return The active {@link AresOctoQuadDriver}, or null if absent.
     */
    public static AresOctoQuadDriver getActiveOctoQuad() {
        return activeOctoQuad;
    }

    /**
     * Initializes the static hardware dependencies required for the ARESLib environment.
     * This method automatically attempts to detect high-performance coprocessors and sets up
     * the system power monitoring.
     *
     * @param hardwareMap The active robot {@link HardwareMap} provided by the current OpMode.
     */
    public static void initHardware(HardwareMap hardwareMap) {
        try {
            // Auto-detect OctoQuad V1
            hardwareMap.get("octoquad_v1"); // Throws exception if not present
            activeOctoQuad = new OctoQuadV1Impl(hardwareMap, "octoquad_v1");
        } catch (IllegalArgumentException e1) {
            try {
                // Fallback to auto-detect OctoQuad V2
                hardwareMap.get("octoquad_v2");
                activeOctoQuad = new OctoQuadV2Impl(hardwareMap, "octoquad_v2");
            } catch (IllegalArgumentException e2) {
                activeOctoQuad = null;
            }
        }

        try {
            // Instantiate SRS Hub
            activeSrsHub = hardwareMap.get(org.areslib.hardware.coprocessors.SRSHub.class, "srshub");
            // Standardizing AresLib wrapper to assume all ports are populated as encoders/analog lines
            org.areslib.hardware.coprocessors.SRSHub.Config srsConfig = new org.areslib.hardware.coprocessors.SRSHub.Config();
            for (int i = 1; i <= 6; i++) {
                srsConfig.setEncoder(i, org.areslib.hardware.coprocessors.SRSHub.Encoder.QUADRATURE);
            }
            for (int i = 1; i <= 12; i++) {
                srsConfig.setAnalogDigitalDevice(i, org.areslib.hardware.coprocessors.SRSHub.AnalogDigitalDevice.ANALOG);
            }
            activeSrsHub.init(srsConfig);
        } catch (IllegalArgumentException e) {
            activeSrsHub = null;
        }

        // Select the VoltageSensor reading the highest voltage.
        // The FTC SDK exposes multiple sensors (12V battery, 3.3V/5V logic rails).
        // Blindly taking the first iterator entry may bind to an internal rail,
        // causing updatePowerStatus() to read ~3.3V and zero out masterPowerScale.
        batteryVoltageSensor = null;
        double maxVoltage = 0.0;
        for (com.qualcomm.robotcore.hardware.VoltageSensor sensor : hardwareMap.voltageSensor) {
            double v = sensor.getVoltage();
            if (v > maxVoltage) {
                maxVoltage = v;
                batteryVoltageSensor = sensor;
            }
        }

        try {
            // Try standard Control Hub / Expansion Hub mapping first
            floodgateSensor = new org.areslib.hardware.wrappers.AnalogInputWrapper(
                hardwareMap.get(AnalogInput.class, "floodgate")
            );
        } catch (IllegalArgumentException e) {
            // If it's not plugged into the standalone native map, try the SRS Hub pin 1
            if (activeSrsHub != null) {
                floodgateSensor = new org.areslib.hardware.wrappers.AresSrsSensor(1, org.areslib.hardware.wrappers.SrsMode.ANALOG);
            } else {
                floodgateSensor = null;
            }
        }
    }

    /**
     * Gets the current, globally cached battery voltage.
     *
     * @return Battery voltage in volts.
     */
    public static double getBatteryVoltage() {
        return batteryVoltage;
    }

    /**
     * Calculates and updates the dynamic power scaling metrics based on current power draw
     * and system voltage. Updates the underlying logging inputs to telemetry.
     */
    public static void updatePowerStatus() {
        if (batteryVoltageSensor != null) {
            batteryVoltage = batteryVoltageSensor.getVoltage();
        }

        if (floodgateSensor != null) {
            // Floodgate V2 maps 0-3.3V to 0-80A
            double rawAmps = (floodgateSensor.getVoltage() / 3.3) * 80.0;
            totalCurrentAmps = (totalCurrentAmps * 0.8) + (rawAmps * 0.2);
        }

        double voltageScale = 1.0;
        if (batteryVoltage > 9.0) {
            voltageScale = 1.0;
        } else if (batteryVoltage >= 7.0 && batteryVoltage <= 9.0) {
            voltageScale = (batteryVoltage - 7.0) / 2.0;
        } else {
            voltageScale = 0.0;
        }

        double currentScale = 1.0;
        if (totalCurrentAmps < 15.0) {
            currentScale = 1.0;
        } else if (totalCurrentAmps <= 19.5) {
            currentScale = 1.0 - ((totalCurrentAmps - 15.0) / 4.5);
        } else {
            currentScale = 0.0;
        }

        masterPowerScale = Math.min(voltageScale, currentScale);
        masterPowerScale = Math.max(0.0, Math.min(1.0, masterPowerScale));

        powerInputs.batteryVoltage = batteryVoltage;
        powerInputs.totalCurrentAmps = totalCurrentAmps;
        powerInputs.masterPowerScale = masterPowerScale;
        AresAutoLogger.processInputs("Power", powerInputs);
    }

    /**
     * Gets one of the nested I2C buses on the SRS Hub.
     *
     * @param bus The integer ID of the bus to access.
     * @return The bus object, currently always null due to unresolved ARES dependencies.
     */
    public static Object getSrsI2cBus(int bus) {
        // Obsolete functionality, we don't bind embedded i2c inside the srs hub wrapper for simplicity currently.
        return null;
    }

    /**
     * Called iteratively to update odometry coprocessors or vision pipelines asynchronously.
     */
    public static void updateCoprocessors() {
        if (activeSrsHub != null) {
            activeSrsHub.update();
        }
        clearCoprocessorCaches();
    }

    /**
     * Forces standard I2C cache refreshes for the bulk reads natively provided by hardware vendors.
     */
    public static void clearCoprocessorCaches() {
        if (activeOctoQuad != null) {
            activeOctoQuad.clearReadCache();
        }
        // SRSHub uses explicit update() model, clearing cache handled natively in the read buffer
    }
}

package org.areslib.telemetry;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

/**
 * Telemetry backend for logging data to FTC Dashboard via network packets.
 * For use strictly when deployed to Android-based FTC control systems.
 */
public class AndroidDashboardBackend implements AresLoggerBackend {
    private final FtcDashboard dashboard;
    private TelemetryPacket currentPacket;

    /**
     * Initializes the Android FTC Dashboard backend connection.
     */
    public AndroidDashboardBackend() {
        dashboard = FtcDashboard.getInstance();
        currentPacket = new TelemetryPacket();
    }

    @Override
    public void putNumber(String key, double value) {
        currentPacket.put(key, value);
    }

    @Override
    public void putNumberArray(String key, double[] values) {
        currentPacket.put(key, values);
    }

    @Override
    public void putString(String key, String value) {
        currentPacket.put(key, value);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        currentPacket.put(key, value);
    }

    @Override
    public void putBooleanArray(String key, boolean[] values) {
        // Fallback to storing as string/object if FTC Dashboard doesn't natively map boolean[]
        currentPacket.put(key, values);
    }

    @Override
    public void putStringArray(String key, String[] values) {
        currentPacket.put(key, values);
    }

    @Override
    public void update() {
        if (dashboard != null) {
            dashboard.sendTelemetryPacket(currentPacket);
        }
        currentPacket = new TelemetryPacket();
    }
}

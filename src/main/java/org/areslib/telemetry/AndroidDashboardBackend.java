package org.areslib.telemetry;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

public class AndroidDashboardBackend implements AresLoggerBackend {
    private final FtcDashboard dashboard;
    private TelemetryPacket currentPacket;

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
    public void update() {
        if (dashboard != null) {
            dashboard.sendTelemetryPacket(currentPacket);
        }
        currentPacket = new TelemetryPacket();
    }
}

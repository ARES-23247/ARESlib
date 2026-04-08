package org.areslib.telemetry;

import com.acmerobotics.dashboard.DashboardCore;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

public class DesktopLiveBackend implements AresLoggerBackend {
  private final DashboardCore dashboard;
  private TelemetryPacket currentPacket;

  public DesktopLiveBackend() {
    // Initializes the local Pure-Java WebSocket server
    dashboard = new DashboardCore();
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

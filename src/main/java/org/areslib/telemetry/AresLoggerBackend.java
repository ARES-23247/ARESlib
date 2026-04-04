package org.areslib.telemetry;

public interface AresLoggerBackend {
    void putNumber(String key, double value);
    void putNumberArray(String key, double[] values);
    void putString(String key, String value);
    void update();
}

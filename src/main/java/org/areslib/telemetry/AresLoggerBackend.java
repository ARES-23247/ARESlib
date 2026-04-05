package org.areslib.telemetry;

/**
 * Interface representing a backend sink for telemetry data.
 */
public interface AresLoggerBackend {
    /**
     * Logs a numeric value.
     * @param key The telemetry key.
     * @param value The value.
     */
    void putNumber(String key, double value);

    /**
     * Logs an array of numbers.
     * @param key The telemetry key.
     * @param values The value array.
     */
    void putNumberArray(String key, double[] values);

    /**
     * Logs a string.
     * @param key The telemetry key.
     * @param value The string value.
     */
    void putString(String key, String value);

    /**
     * Logs serialized structural data.
     * @param key The telemetry key.
     * @param typeString Structural metadata.
     * @param data Raw structural bytes.
     */
    default void putStruct(String key, String typeString, byte[] data) {}

    /**
     * Triggers the backend to flush/send current packets.
     */
    void update();
}

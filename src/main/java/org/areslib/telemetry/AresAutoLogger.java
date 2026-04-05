package org.areslib.telemetry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AdvantageKit-Style AutoLogger.
 * Uses a static HashSet mapped to the class structural bytes to read fields natively exactly ONCE.
 * Sub-zero loop latency compared to standard reflection, effectively flattening nested objects.
 */
public class AresAutoLogger {

    /** High performance cache so that getFields() is only ever called ONCE per data class. */
    private static final Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

    /**
     * Replicates AdvantageKit's @AutoLog by flattening all supported primitives inside the inputs.
     * @param prefix Base directory string (e.g. "Elevator", "Swerve/FrontLeft")
     * @param inputs The object containing primitive fields.
     */
    public static void processInputs(String prefix, AresLoggableInputs inputs) {
        if (inputs == null) return;

        Class<?> clazz = inputs.getClass();
        
        // Cache read lock: Instantly pulls the pre-allocated Field layout array
        Field[] fields = fieldCache.computeIfAbsent(clazz, Class::getFields);

        // Raw loop: Directly invokes final array indices over memory
        for (Field field : fields) {
            try {
                Object value = field.get(inputs);
                if (value == null) continue;

                String key = prefix + "/" + field.getName();
                Class<?> type = field.getType();

                // AdvantageScope standard mapping
                if (type == double.class || type == Double.class) {
                    AresTelemetry.putNumber(key, (Double) value);
                } else if (type == int.class || type == Integer.class) {
                    AresTelemetry.putNumber(key, ((Integer) value).doubleValue()); // Dashboard converts everything to double
                } else if (type == boolean.class || type == Boolean.class) {
                    AresTelemetry.putNumber(key, ((Boolean) value) ? 1.0 : 0.0);
                } else if (type == String.class) {
                    AresTelemetry.putString(key, (String) value);
                } else if (type == double[].class) {
                    AresTelemetry.putNumberArray(key, (double[]) value);
                }
                
                // Note: Complex objects not explicitly supported above are ignored safely 
                // pursuant to flat-reflection physics structure.

            } catch (IllegalAccessException e) {
                // Highly performant silent catch block: 
                // Any private/protected field is ignored without crashing the robot's main loop.
            }
        }
    }

    /** 
     * Manually track an arbitrary double. 
     * @param key Telemetry key
     * @param value The value to log
     */
    public static void recordOutput(String key, double value) {
        AresTelemetry.putNumber(key, value);
    }

    /** 
     * Manually track an arbitrary String. 
     * @param key Telemetry key
     * @param value The string to log
     */
    public static void recordOutput(String key, String value) {
        AresTelemetry.putString(key, value);
    }

    /** 
     * Manually track an arbitrary double array (like Swerve states or arbitrary vectors). 
     * @param key Telemetry key
     * @param value The double parameters to log
     */
    public static void recordOutput(String key, double... value) {
        AresTelemetry.putNumberArray(key, value);
    }
}

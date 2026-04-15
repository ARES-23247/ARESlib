package org.areslib.telemetry;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.areslib.math.kinematics.SwerveModuleState;

/**
 * AdvantageKit-Style AutoLogger.
 *
 * <p>Hardened for zero-allocation in the hot path. Caches field layouts and pre-concatenated keys
 * per (prefix + class) combination. Uses non-boxing primitive access where possible.
 */
public class AresAutoLogger {

  /**
   * Annotation used to mark IO Input classes for automatic telemetry logging.
   *
   * <p>When an object implementing {@link AresLoggableInputs} is passed to {@link #processInputs},
   * all declared fields will be flattened into the telemetry stream.
   */
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
  public @interface AutoLog {}

  /** Unified entry for a single field being logged. */
  private static class LogEntry {
    final Field field;
    final String key;
    final LogType type;
    double[] arrayCache; // Cached for SwerveModuleState array logging

    LogEntry(Field field, String key) {
      this.field = field;
      this.key = key;
      this.type = LogType.fromClass(field.getType());
      this.field.setAccessible(true);
    }
  }

  private enum LogType {
    DOUBLE,
    INT,
    BOOLEAN,
    STRING,
    DOUBLE_ARRAY,
    SWERVE_STATES,
    UNKNOWN;

    static LogType fromClass(Class<?> clazz) {
      if (clazz == double.class || clazz == Double.class) return DOUBLE;
      if (clazz == int.class || clazz == Integer.class) return INT;
      if (clazz == boolean.class || clazz == Boolean.class) return BOOLEAN;
      if (clazz == String.class) return STRING;
      if (clazz == double[].class) return DOUBLE_ARRAY;
      if (clazz == SwerveModuleState[].class) return SWERVE_STATES;
      return UNKNOWN;
    }
  }

  /** Cache of entries per prefix+class combination — eliminates per-cycle key concatenation. */
  private static final Map<String, List<LogEntry>> ENTRY_CACHE = new ConcurrentHashMap<>();

  /**
   * Replicates AdvantageKit's @AutoLog by flattening all supported primitives inside the inputs.
   *
   * <p>Optimized for zero GC pressure via persistent entry caching.
   *
   * @param prefix Base directory string (e.g. "Elevator", "Swerve/FrontLeft")
   * @param inputs The object containing primitive fields.
   */
  public static void processInputs(String prefix, AresLoggableInputs inputs) {
    if (inputs == null) return;

    String cacheKey = prefix + "_" + inputs.getClass().getName();
    List<LogEntry> entries =
        ENTRY_CACHE.computeIfAbsent(
            cacheKey,
            k -> {
              List<LogEntry> list = new ArrayList<>();
              Field[] fields = inputs.getClass().getDeclaredFields();
              for (Field f : fields) {
                list.add(new LogEntry(f, prefix + "/" + f.getName()));
              }
              return list;
            });

    for (int i = 0; i < entries.size(); i++) {
      LogEntry entry = entries.get(i);
      try {
        switch (entry.type) {
          case DOUBLE:
            // Use primitive access to avoid Double boxing
            AresTelemetry.putNumber(entry.key, entry.field.getDouble(inputs));
            break;

          case INT:
            AresTelemetry.putNumber(entry.key, (double) entry.field.getInt(inputs));
            break;

          case BOOLEAN:
            AresTelemetry.putNumber(entry.key, entry.field.getBoolean(inputs) ? 1.0 : 0.0);
            break;

          case STRING:
            Object strVal = entry.field.get(inputs);
            if (strVal != null) AresTelemetry.putString(entry.key, (String) strVal);
            break;

          case DOUBLE_ARRAY:
            double[] arr = (double[]) entry.field.get(inputs);
            if (arr != null) AresTelemetry.putNumberArray(entry.key, arr);
            break;

          case SWERVE_STATES:
            SwerveModuleState[] states = (SwerveModuleState[]) entry.field.get(inputs);
            if (states != null) {
              if (entry.arrayCache == null || entry.arrayCache.length != states.length * 2) {
                entry.arrayCache = new double[states.length * 2];
              }
              for (int j = 0; j < states.length; j++) {
                entry.arrayCache[j * 2] = states[j].angle.getRadians();
                entry.arrayCache[j * 2 + 1] = states[j].speedMetersPerSecond;
              }
              AresTelemetry.putNumberArray(entry.key, entry.arrayCache);
            }
            break;

          default:
            break;
        }
      } catch (IllegalAccessException e) {
        com.qualcomm.robotcore.util.RobotLog.addGlobalWarningMessage(
            "AresAutoLogger field access failed: " + e.getMessage());
      }
    }
  }

  /**
   * Manually track an arbitrary double.
   *
   * @param key Telemetry key
   * @param value The value to log
   */
  public static void recordOutput(String key, double value) {
    AresTelemetry.putNumber(key, value);
  }

  /**
   * Manually track an arbitrary String.
   *
   * @param key Telemetry key
   * @param value The string to log
   */
  public static void recordOutput(String key, String value) {
    AresTelemetry.putString(key, value);
  }

  /**
   * Manually track an arbitrary double array (like Swerve states or arbitrary vectors).
   *
   * @param key Telemetry key
   * @param values The double array to log
   */
  public static void recordOutputArray(String key, double... values) {
    AresTelemetry.putNumberArray(key, values);
  }

  /**
   * Manually track an array of Strings (like Active Faults).
   *
   * @param key Telemetry key
   * @param values The string array to log
   */
  public static void recordOutput(String key, String[] values) {
    AresTelemetry.putStringArray(key, values);
  }
}

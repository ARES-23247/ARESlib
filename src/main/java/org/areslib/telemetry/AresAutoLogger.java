package org.areslib.telemetry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AdvantageKit-Style AutoLogger. Caches reflected field layouts per class so {@code getFields()} is
 * only called once, then iterates over the cached array for subsequent calls. Supports primitive
 * types, Strings, double arrays, and SwerveModuleState arrays.
 */
public class AresAutoLogger {

  /** Per-class field layout cache — avoids repeated reflection after first access. */
  private static final Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

  /**
   * Replicates AdvantageKit's @AutoLog by flattening all supported primitives inside the inputs.
   *
   * @param prefix Base directory string (e.g. "Elevator", "Swerve/FrontLeft")
   * @param inputs The object containing primitive fields.
   */
  public static void processInputs(String prefix, AresLoggableInputs inputs) {
    if (inputs == null) return;

    Class<?> clazz = inputs.getClass();

    // Lazily cache the declared fields on first access for this class
    Field[] fields =
        fieldCache.computeIfAbsent(
            clazz,
            c -> {
              Field[] flds = c.getDeclaredFields();
              for (Field f : flds) {
                f.setAccessible(true);
              }
              return flds;
            });

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
          AresTelemetry.putNumber(
              key, ((Integer) value).doubleValue()); // Dashboard converts everything to double
        } else if (type == boolean.class || type == Boolean.class) {
          AresTelemetry.putNumber(key, ((Boolean) value) ? 1.0 : 0.0);
        } else if (type == String.class) {
          AresTelemetry.putString(key, (String) value);
        } else if (type == double[].class) {
          AresTelemetry.putNumberArray(key, (double[]) value);
        } else if (type == org.areslib.math.kinematics.SwerveModuleState[].class) {
          org.areslib.math.kinematics.SwerveModuleState[] states =
              (org.areslib.math.kinematics.SwerveModuleState[]) value;
          double[] arr = new double[states.length * 2];
          for (int i = 0; i < states.length; i++) {
            // AdvantageScope Swerve format: [angle, speed, angle, speed, ...]
            arr[i * 2] = states[i].angle.getRadians();
            arr[i * 2 + 1] = states[i].speedMetersPerSecond;
          }
          AresTelemetry.putNumberArray(key, arr);
        }

        // Unsupported types are silently skipped.

      } catch (IllegalAccessException e) {
        // Inaccessible fields are skipped without crashing the robot loop.
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

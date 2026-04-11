package org.areslib.telemetry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.areslib.math.kinematics.DifferentialDriveWheelSpeeds;
import org.areslib.math.kinematics.MecanumDriveWheelSpeeds;
import org.areslib.math.kinematics.SwerveModuleState;

/**
 * Global telemetry distribution hub. Routes log data to all registered backend implementations
 * (e.g., FtcDashboard, wpilog).
 */
public class AresTelemetry {
  private static final List<AresLoggerBackend> BACKENDS = new CopyOnWriteArrayList<>();

  /**
   * Registers a new telemetry backend to receive data. Uses class-based deduplication so that
   * re-registering the same backend type (e.g., across OpMode transitions) replaces the old
   * instance instead of duplicating.
   *
   * @param backend The backend implementation to register.
   */
  public static void registerBackend(AresLoggerBackend backend) {
    // Remove any existing backend of the same class to prevent accumulation
    BACKENDS.removeIf(existing -> existing.getClass().equals(backend.getClass()));
    BACKENDS.add(backend);
  }

  /**
   * Removes all registered BACKENDS. Should be called during scheduler reset to prevent stale
   * backend accumulation across OpMode transitions.
   */
  public static void clearBackends() {
    BACKENDS.clear();
  }

  /**
   * Puts a number value into telemetry.
   *
   * @param key The telemetry key.
   * @param value The value.
   */
  public static void putNumber(String key, double value) {
    for (AresLoggerBackend backend : BACKENDS) {
      backend.putNumber(key, value);
    }
  }

  /**
   * Puts an array of numbers into telemetry.
   *
   * @param key The telemetry key.
   * @param values The values array.
   */
  public static void putNumberArray(String key, double[] values) {
    for (AresLoggerBackend backend : BACKENDS) {
      backend.putNumberArray(key, values);
    }
  }

  /**
   * Puts a string value into telemetry.
   *
   * @param key The telemetry key.
   * @param value The value.
   */
  public static void putString(String key, String value) {
    for (AresLoggerBackend backend : BACKENDS) {
      backend.putString(key, value);
    }
  }

  /**
   * Puts a boolean value into telemetry.
   *
   * @param key The telemetry key.
   * @param value The value.
   */
  public static void putBoolean(String key, boolean value) {
    for (AresLoggerBackend backend : BACKENDS) {
      backend.putBoolean(key, value);
    }
  }

  /**
   * Puts an array of booleans into telemetry.
   *
   * @param key The telemetry key.
   * @param values The values array.
   */
  public static void putBooleanArray(String key, boolean[] values) {
    for (AresLoggerBackend backend : BACKENDS) {
      backend.putBooleanArray(key, values);
    }
  }

  /**
   * Puts an array of strings into telemetry.
   *
   * @param key The telemetry key.
   * @param values The values array.
   */
  public static void putStringArray(String key, String[] values) {
    for (AresLoggerBackend backend : BACKENDS) {
      backend.putStringArray(key, values);
    }
  }

  /** Updates all registered BACKENDS. Should be called periodically. */
  public static void update() {
    for (AresLoggerBackend backend : BACKENDS) {
      backend.update();
    }
  }

  // Helper methods ported from the old AresLogger

  /**
   * Logs a Pose2d into telemetry as a double array.
   *
   * @param key The telemetry key.
   * @param xMeters The X position in meters.
   * @param yMeters The Y position in meters.
   * @param headingRadians The heading in radians.
   */
  public static void putPose2d(String key, double xMeters, double yMeters, double headingRadians) {
    putNumberArray(key, new double[] {xMeters, yMeters, headingRadians});
  }

  /**
   * Logs exactly 4 SwerveModuleState elements as a double array in AdvantageScope format.
   *
   * @param key The telemetry key.
   * @param states Array of 4 swerve module states.
   */
  public static void logSwerveStates(String key, SwerveModuleState[] states) {
    if (states.length != 4) return;
    double[] array = new double[8];
    for (int i = 0; i < 4; i++) {
      array[i * 2] = states[i].angle.getRadians(); // Angle first for AdvantageScope
      array[i * 2 + 1] = states[i].speedMetersPerSecond;
    }
    putNumberArray(key, array);
  }

  /**
   * Logs differential drive speeds as an array for AdvantageScope.
   *
   * @param key The telemetry key.
   * @param speeds The wheel speeds.
   */
  public static void logDifferentialSpeeds(String key, DifferentialDriveWheelSpeeds speeds) {
    double[] stateArray = new double[] {speeds.leftMetersPerSecond, speeds.rightMetersPerSecond};
    putNumberArray(key, stateArray);
  }

  /**
   * Logs mecanum drive speeds as an array for AdvantageScope.
   *
   * @param key The telemetry key.
   * @param speeds The wheel speeds.
   */
  public static void logMecanumSpeeds(String key, MecanumDriveWheelSpeeds speeds) {
    double[] stateArray =
        new double[] {
          speeds.frontLeftMetersPerSecond, speeds.frontRightMetersPerSecond,
          speeds.rearLeftMetersPerSecond, speeds.rearRightMetersPerSecond
        };
    putNumberArray(key, stateArray);
  }
}

package org.areslib.telemetry;

import org.areslib.math.kinematics.DifferentialDriveWheelSpeeds;
import org.areslib.math.kinematics.MecanumDriveWheelSpeeds;
import org.areslib.math.kinematics.SwerveModuleState;

import java.util.ArrayList;
import java.util.List;

/**
 * Global telemetry distribution hub.
 * Routes log data to all registered backend implementations (e.g., FtcDashboard, wpilog).
 */
public class AresTelemetry {
    private static final List<AresLoggerBackend> backends = new ArrayList<>();

    /**
     * Registers a new telemetry backend to receive data.
     * @param backend The backend implementation to register.
     */
    public static void registerBackend(AresLoggerBackend backend) {
        if (!backends.contains(backend)) {
            backends.add(backend);
        }
    }

    /**
     * Puts a number value into telemetry.
     * @param key The telemetry key.
     * @param value The value.
     */
    public static void putNumber(String key, double value) {
        for (AresLoggerBackend backend : backends) {
            backend.putNumber(key, value);
        }
    }

    /**
     * Puts an array of numbers into telemetry.
     * @param key The telemetry key.
     * @param values The values array.
     */
    public static void putNumberArray(String key, double[] values) {
        for (AresLoggerBackend backend : backends) {
            backend.putNumberArray(key, values);
        }
    }

    /**
     * Puts a string value into telemetry.
     * @param key The telemetry key.
     * @param value The value.
     */
    public static void putString(String key, String value) {
        for (AresLoggerBackend backend : backends) {
            backend.putString(key, value);
        }
    }

    /**
     * Updates all registered backends. Should be called periodically.
     */
    public static void update() {
        for (AresLoggerBackend backend : backends) {
            backend.update();
        }
    }

    // Helper methods ported from the old AresLogger

    /**
     * Logs a Pose2d into telemetry as a double array.
     * @param key The telemetry key.
     * @param xMeters The X position in meters.
     * @param yMeters The Y position in meters.
     * @param headingRadians The heading in radians.
     */
    public static void putPose2d(String key, double xMeters, double yMeters, double headingRadians) {
        putNumberArray(key, new double[] { xMeters, yMeters, headingRadians });
    }

    /**
     * Logs exactly 4 SwerveModuleState elements as a double array in AdvantageScope format.
     * @param key The telemetry key.
     * @param states Array of 4 swerve module states.
     */
    public static void logSwerveStates(String key, SwerveModuleState[] states) {
        if (states.length != 4) return;
        double[] array = new double[8];
        for (int i = 0; i < 4; i++) {
            array[i * 2] = states[i].speedMetersPerSecond;
            array[i * 2 + 1] = states[i].angle.getRadians();
        }
        putNumberArray(key, array);
    }

    /**
     * Logs differential drive speeds as an array for AdvantageScope.
     * @param key The telemetry key.
     * @param speeds The wheel speeds.
     */
    public static void logDifferentialSpeeds(String key, DifferentialDriveWheelSpeeds speeds) {
        double[] stateArray = new double[] { speeds.leftMetersPerSecond, speeds.rightMetersPerSecond };
        putNumberArray(key, stateArray);
    }

    /**
     * Logs mecanum drive speeds as an array for AdvantageScope.
     * @param key The telemetry key.
     * @param speeds The wheel speeds.
     */
    public static void logMecanumSpeeds(String key, MecanumDriveWheelSpeeds speeds) {
        double[] stateArray = new double[] { 
            speeds.frontLeftMetersPerSecond, speeds.frontRightMetersPerSecond,
            speeds.rearLeftMetersPerSecond, speeds.rearRightMetersPerSecond 
        };
        putNumberArray(key, stateArray);
    }
}

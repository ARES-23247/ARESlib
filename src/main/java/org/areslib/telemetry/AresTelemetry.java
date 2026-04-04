package org.areslib.telemetry;

import org.areslib.math.kinematics.DifferentialDriveWheelSpeeds;
import org.areslib.math.kinematics.MecanumDriveWheelSpeeds;
import org.areslib.math.kinematics.SwerveModuleState;

import java.util.ArrayList;
import java.util.List;

public class AresTelemetry {
    private static final List<AresLoggerBackend> backends = new ArrayList<>();

    public static void registerBackend(AresLoggerBackend backend) {
        backends.add(backend);
    }

    public static void putNumber(String key, double value) {
        for (AresLoggerBackend backend : backends) {
            backend.putNumber(key, value);
        }
    }

    public static void putNumberArray(String key, double[] values) {
        for (AresLoggerBackend backend : backends) {
            backend.putNumberArray(key, values);
        }
    }

    public static void putString(String key, String value) {
        for (AresLoggerBackend backend : backends) {
            backend.putString(key, value);
        }
    }

    public static void update() {
        for (AresLoggerBackend backend : backends) {
            backend.update();
        }
    }

    // Helper methods ported from the old AresLogger

    /**
     * Logs exactly 4 SwerveModuleState elements as an 8-double array.
     * Required AdvantageScope native formatting: [Angle0, Speed0, Angle1, Speed1, ...]
     */
    public static void logSwerveStates(String key, SwerveModuleState[] states) {
        if (states.length != 4) return;
        double[] stateArray = new double[8];
        for (int i = 0; i < 4; i++) {
            stateArray[i * 2] = states[i].angle.getRadians();
            stateArray[i * 2 + 1] = states[i].speedMetersPerSecond;
        }
        putNumberArray(key, stateArray);
    }

    public static void logDifferentialSpeeds(String key, DifferentialDriveWheelSpeeds speeds) {
        double[] stateArray = new double[] { speeds.leftMetersPerSecond, speeds.rightMetersPerSecond };
        putNumberArray(key, stateArray);
    }

    public static void logMecanumSpeeds(String key, MecanumDriveWheelSpeeds speeds) {
        double[] stateArray = new double[] { 
            speeds.frontLeftMetersPerSecond, speeds.frontRightMetersPerSecond,
            speeds.rearLeftMetersPerSecond, speeds.rearRightMetersPerSecond 
        };
        putNumberArray(key, stateArray);
    }
}

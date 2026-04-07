package org.areslib.telemetry;

import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.kinematics.DifferentialDriveWheelSpeeds;
import org.areslib.math.kinematics.MecanumDriveWheelSpeeds;
import org.areslib.math.kinematics.SwerveModuleState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AresTelemetryTest {

    static class MockBackend implements AresLoggerBackend {
        public double lastNum = 0.0;
        public String lastString = "";
        public double[] lastNumArr = null;

        @Override public void putNumber(String key, double value) { lastNum = value; }
        @Override public void putNumberArray(String key, double[] values) { lastNumArr = values; }
        @Override public void putString(String key, String value) { lastString = value; }
        @Override public void putBoolean(String key, boolean value) { }
        @Override public void putBooleanArray(String key, boolean[] values) { }
        @Override public void putStringArray(String key, String[] values) { }
        @Override public void update() {}
    }

    private MockBackend mockBackend;

    @BeforeEach
    void setUp() {
        AresTelemetry.clearBackends();
        mockBackend = new MockBackend();
        AresTelemetry.registerBackend(mockBackend);
    }

    @AfterEach
    void tearDown() {
        AresTelemetry.clearBackends();
    }

    @Test
    void testPutNumber() {
        AresTelemetry.putNumber("Test/Key", 42.5);
        assertEquals(42.5, mockBackend.lastNum);
    }

    @Test
    void testPutString() {
        AresTelemetry.putString("Test/String", "Ares");
        assertEquals("Ares", mockBackend.lastString);
    }

    @Test
    void testPose2dLogging() {
        AresTelemetry.putPose2d("Odom/Pose", 1.5, -2.5, Math.PI / 2.0);
        assertNotNull(mockBackend.lastNumArr);
        assertEquals(3, mockBackend.lastNumArr.length);
        assertEquals(1.5, mockBackend.lastNumArr[0]);
        assertEquals(-2.5, mockBackend.lastNumArr[1]);
        assertEquals(Math.PI / 2.0, mockBackend.lastNumArr[2]);
    }

    @Test
    void testDifferentialDriveSpeeds() {
        DifferentialDriveWheelSpeeds speeds = new DifferentialDriveWheelSpeeds(1.2, 1.4);
        AresTelemetry.logDifferentialSpeeds("Drive/Diff", speeds);
        assertNotNull(mockBackend.lastNumArr);
        assertEquals(2, mockBackend.lastNumArr.length);
        assertEquals(1.2, mockBackend.lastNumArr[0]);
        assertEquals(1.4, mockBackend.lastNumArr[1]);
    }

    @Test
    void testMecanumSpeeds() {
        MecanumDriveWheelSpeeds speeds = new MecanumDriveWheelSpeeds(1.0, 1.1, 1.2, 1.3);
        AresTelemetry.logMecanumSpeeds("Drive/Mecanum", speeds);
        assertNotNull(mockBackend.lastNumArr);
        assertEquals(4, mockBackend.lastNumArr.length);
        assertEquals(1.0, mockBackend.lastNumArr[0]);
        assertEquals(1.1, mockBackend.lastNumArr[1]);
        assertEquals(1.2, mockBackend.lastNumArr[2]);
        assertEquals(1.3, mockBackend.lastNumArr[3]);
    }

    @Test
    void testSwerveStatesLogging() {
        SwerveModuleState[] states = new SwerveModuleState[] {
            new SwerveModuleState(1.5, new Rotation2d(0.5)),
            new SwerveModuleState(1.5, new Rotation2d(0.5)),
            new SwerveModuleState(1.5, new Rotation2d(0.5)),
            new SwerveModuleState(1.5, new Rotation2d(0.5))
        };
        AresTelemetry.logSwerveStates("Drive/Swerve", states);
        assertNotNull(mockBackend.lastNumArr);
        assertEquals(8, mockBackend.lastNumArr.length);
        assertEquals(0.5, mockBackend.lastNumArr[0]);
        assertEquals(1.5, mockBackend.lastNumArr[1]);
    }
}

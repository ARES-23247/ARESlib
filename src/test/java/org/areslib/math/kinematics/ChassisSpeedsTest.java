package org.areslib.math.kinematics;

import org.areslib.math.geometry.Rotation2d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ChassisSpeeds}.
 */
class ChassisSpeedsTest {

    private static final double EPSILON = 1e-6;

    @Test
    @DisplayName("Default constructor produces zero speeds")
    void defaultConstructor() {
        ChassisSpeeds speeds = new ChassisSpeeds();
        assertEquals(0.0, speeds.vxMetersPerSecond, EPSILON);
        assertEquals(0.0, speeds.vyMetersPerSecond, EPSILON);
        assertEquals(0.0, speeds.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("fromFieldRelativeSpeeds at zero heading is identity")
    void fieldRelativeZeroHeading() {
        ChassisSpeeds speeds = ChassisSpeeds.fromFieldRelativeSpeeds(1.0, 2.0, 0.5, new Rotation2d(0));
        assertEquals(1.0, speeds.vxMetersPerSecond, EPSILON);
        assertEquals(2.0, speeds.vyMetersPerSecond, EPSILON);
        assertEquals(0.5, speeds.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("fromFieldRelativeSpeeds rotates correctly at 90 degrees")
    void fieldRelative90Degrees() {
        ChassisSpeeds speeds = ChassisSpeeds.fromFieldRelativeSpeeds(
            1.0, 0.0, 0.0, new Rotation2d(Math.PI / 2));
        // At 90°, field +X → robot +Y
        assertEquals(0.0, speeds.vxMetersPerSecond, EPSILON);
        assertEquals(-1.0, speeds.vyMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("discretize with zero dt returns original speeds")
    void discretizeZeroDt() {
        ChassisSpeeds speeds = new ChassisSpeeds(1.0, 2.0, 0.5);
        ChassisSpeeds result = ChassisSpeeds.discretize(speeds, 0.0);
        assertEquals(1.0, result.vxMetersPerSecond, EPSILON);
        assertEquals(2.0, result.vyMetersPerSecond, EPSILON);
        assertEquals(0.5, result.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("discretize with negative dt returns original speeds")
    void discretizeNegativeDt() {
        ChassisSpeeds speeds = new ChassisSpeeds(1.0, 2.0, 0.5);
        ChassisSpeeds result = ChassisSpeeds.discretize(speeds, -0.01);
        assertEquals(1.0, result.vxMetersPerSecond, EPSILON);
        assertEquals(2.0, result.vyMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("discretize with zero omega returns original speeds")
    void discretizeZeroOmega() {
        ChassisSpeeds speeds = new ChassisSpeeds(1.0, 2.0, 0.0);
        ChassisSpeeds result = ChassisSpeeds.discretize(speeds, 0.02);
        assertEquals(1.0, result.vxMetersPerSecond, EPSILON);
        assertEquals(2.0, result.vyMetersPerSecond, EPSILON);
    }

    @Test
    @DisplayName("discretize corrects drift during rotation")
    void discretizeCorrectionDuringRotation() {
        // High omega means naive integration would drift
        ChassisSpeeds speeds = new ChassisSpeeds(2.0, 0.0, Math.PI);
        ChassisSpeeds result = ChassisSpeeds.discretize(speeds, 0.02);
        // The discretized vx should differ slightly from original
        assertNotEquals(speeds.vxMetersPerSecond, result.vxMetersPerSecond, 1e-4);
        // But omega remains the same
        assertEquals(Math.PI, result.omegaRadiansPerSecond, EPSILON);
    }

    @Test
    @DisplayName("discretize produces no NaN or Infinity")
    void discretizeNoNaN() {
        ChassisSpeeds speeds = new ChassisSpeeds(1.0, 1.0, 10.0);
        ChassisSpeeds result = ChassisSpeeds.discretize(speeds, 0.02);
        assertFalse(Double.isNaN(result.vxMetersPerSecond));
        assertFalse(Double.isNaN(result.vyMetersPerSecond));
        assertFalse(Double.isInfinite(result.vxMetersPerSecond));
        assertFalse(Double.isInfinite(result.vyMetersPerSecond));
    }
}

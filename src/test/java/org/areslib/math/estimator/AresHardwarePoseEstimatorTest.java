package org.areslib.math.estimator;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AresHardwarePoseEstimatorTest {

    private static final double EPSILON = 1e-6;
    private AresHardwarePoseEstimator estimator;

    @BeforeEach
    void setUp() {
        Pose2d initialHardwarePose = new Pose2d(0, 0, new Rotation2d(0));
        Pose2d initialEstimatedPose = new Pose2d(0, 0, new Rotation2d(0));
        estimator = new AresHardwarePoseEstimator(initialHardwarePose, initialEstimatedPose);
    }

    @Test
    @DisplayName("Estimator starts at initial estimated pose")
    void startsAtInitialPose() {
        Pose2d currentPos = estimator.getEstimatedPosition();
        assertEquals(0.0, currentPos.getX(), EPSILON);
        assertEquals(0.0, currentPos.getY(), EPSILON);
    }

    @Test
    @DisplayName("Update correctly applies hardware delta")
    void updateAppliesHardwareDelta() {
        // Hardware reports we moved to X=1
        Pose2d hardwareReport = new Pose2d(1.0, 0.0, new Rotation2d(0.0));
        Pose2d newEstimatedPos = estimator.update(hardwareReport, 0.5);

        assertEquals(1.0, newEstimatedPos.getX(), EPSILON);
        assertEquals(1.0, estimator.getEstimatedPosition().getX(), EPSILON);
    }

    @Test
    @DisplayName("Vision measurement correctly rewinds and applies offset")
    void visionMeasurementAppliesCorrectly() {
        // Drive to X=1 at t=0.5
        estimator.update(new Pose2d(1.0, 0.0, new Rotation2d(0.0)), 0.5);
        
        // Drive to X=2 at t=1.0
        estimator.update(new Pose2d(2.0, 0.0, new Rotation2d(0.0)), 1.0);

        // At t=0.5, Vision saw us at X=1.5 instead of 1.0
        // Because stdDevs default to [0.1, 0.1, 0.1], the weight kX is 1/(1+0.1) = ~0.909
        // Original estimate at t=0.5 was X=1.0. Error = 1.5 - 1.0 = +0.5. 
        // Corrected at t=0.5: 1.0 + 0.5*0.909 = 1.4545...
        // Replay +1.0 from t=0.5 to t=1.0 -> Final X Should be ~2.4545...
        
        Pose2d visionPose = new Pose2d(1.5, 0.0, new Rotation2d(0.0));
        estimator.addVisionMeasurement(visionPose, 0.5);

        Pose2d correctedPose = estimator.getEstimatedPosition();
        
        // We use a delta of 0.01 to verify the mathematical jump
        assertTrue(correctedPose.getX() > 2.4 && correctedPose.getX() < 2.50, 
                "Pose X should be around 2.45 after vision fusion but was " + correctedPose.getX());
    }

    @Test
    @DisplayName("Invalid stdDev array length throws exception")
    void invalidStdDevLengthThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            estimator.setVisionMeasurementStdDevs(new double[]{0.1, 0.1});
        });
    }

    @Test
    @DisplayName("Resetting position zero-outs the buffer and states")
    void resetPositionClearsState() {
        estimator.update(new Pose2d(5.0, 0.0, new Rotation2d(0.0)), 1.0);
        
        estimator.resetPosition(new Pose2d(0.0, 0.0, new Rotation2d(0.0)), 
                                new Pose2d(10.0, 10.0, new Rotation2d(0.0)));
                                
        Pose2d newEstimated = estimator.getEstimatedPosition();
        assertEquals(10.0, newEstimated.getX(), EPSILON);
        assertEquals(10.0, newEstimated.getY(), EPSILON);
    }
}

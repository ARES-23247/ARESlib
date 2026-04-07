package org.areslib.math.estimator;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Twist2d;
import org.areslib.math.kinematics.MecanumDriveKinematics;
import org.areslib.math.kinematics.MecanumDriveWheelPositions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MecanumDrivePoseEstimatorTest {

    private static final double EPSILON = 1e-6;
    private MecanumDriveKinematics mockKinematics;
    private MecanumDrivePoseEstimator estimator;

    @BeforeEach
    void setUp() {
        mockKinematics = Mockito.mock(MecanumDriveKinematics.class);
        
        MecanumDriveWheelPositions initialPositions = 
            new MecanumDriveWheelPositions(0.0, 0.0, 0.0, 0.0);
        Pose2d initialPose = new Pose2d(0.0, 0.0, new Rotation2d(0.0));
        
        estimator = new MecanumDrivePoseEstimator(mockKinematics, new Rotation2d(0.0), initialPositions, initialPose);
    }

    @Test
    @DisplayName("Estimator starts at initial estimated pose")
    void startsAtInitialPose() {
        Pose2d currentPos = estimator.getEstimatedPosition();
        assertEquals(0.0, currentPos.getX(), EPSILON);
        assertEquals(0.0, currentPos.getY(), EPSILON);
    }

    @Test
    @DisplayName("Update applies kinematic twist delta")
    void updateAppliesKinematicStep() {
        when(mockKinematics.toTwist2d(any(), any())).thenReturn(new Twist2d(1.0, 0.0, 0.0));
        
        MecanumDriveWheelPositions newPositions = 
            new MecanumDriveWheelPositions(1.0, 1.0, 1.0, 1.0);
        
        Pose2d newEstimatedPos = estimator.update(new Rotation2d(0.0), newPositions, 0.5);

        assertEquals(1.0, newEstimatedPos.getX(), EPSILON);
        assertEquals(1.0, estimator.getEstimatedPosition().getX(), EPSILON);
    }

    @Test
    @DisplayName("Vision measurement applies correctly with history buffer")
    void visionMeasurementAppliesCorrectly() {
        // Move to X=1 at t=0.5
        when(mockKinematics.toTwist2d(any(), any())).thenReturn(new Twist2d(1.0, 0.0, 0.0));
        estimator.update(new Rotation2d(0.0), new MecanumDriveWheelPositions(1.0, 1.0, 1.0, 1.0), 0.5);
        
        // Move to X=2 at t=1.0
        when(mockKinematics.toTwist2d(any(), any())).thenReturn(new Twist2d(1.0, 0.0, 0.0));
        estimator.update(new Rotation2d(0.0), new MecanumDriveWheelPositions(2.0, 2.0, 2.0, 2.0), 1.0);

        Pose2d visionPose = new Pose2d(1.5, 0.0, new Rotation2d(0.0));
        estimator.addVisionMeasurement(visionPose, 0.5);

        Pose2d correctedPose = estimator.getEstimatedPosition();
        assertTrue(correctedPose.getX() > 2.4 && correctedPose.getX() < 2.50, 
                "Pose X should be around 2.45 after fusion but was " + correctedPose.getX());
    }

    @Test
    @DisplayName("Resetting position zero-outs the odometry states")
    void resetPositionClearsState() {
        when(mockKinematics.toTwist2d(any(), any())).thenReturn(new Twist2d(5.0, 0.0, 0.0));
        estimator.update(new Rotation2d(0.0), new MecanumDriveWheelPositions(5.0, 5.0, 5.0, 5.0), 1.0);
        
        estimator.resetPosition(new Rotation2d(0.0), 
                                new MecanumDriveWheelPositions(0.0, 0.0, 0.0, 0.0), 
                                new Pose2d(10.0, 10.0, new Rotation2d(0.0)));
                                
        Pose2d newEstimated = estimator.getEstimatedPosition();
        assertEquals(10.0, newEstimated.getX(), EPSILON);
        assertEquals(10.0, newEstimated.getY(), EPSILON);
    }
}

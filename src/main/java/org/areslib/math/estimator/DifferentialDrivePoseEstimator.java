package org.areslib.math.estimator;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.TimeInterpolatableBuffer;
import org.areslib.math.geometry.Twist2d;
import org.areslib.math.kinematics.DifferentialDriveKinematics;
import org.areslib.math.kinematics.DifferentialDriveOdometry;
import org.areslib.math.kinematics.DifferentialDriveWheelPositions;

/**
 * A sophisticated pose estimator for Differential Drives mirroring elite algorithms (WPILib 2024).
 * Uses exact exact module position tracking and a History Buffer for true latency-compensating Vision fusion.
 */
public class DifferentialDrivePoseEstimator {
    private final DifferentialDriveOdometry m_odometry;
    private final TimeInterpolatableBuffer<Pose2d> m_poseBuffer;
    
    private Pose2d m_estimatedPose;
    private double[] m_visionStdDevs = new double[] { 0.1, 0.1, 0.1 };

    /**
     * Constructs a DifferentialDrivePoseEstimator.
     *
     * @param kinematics A correctly-configured kinematics object for your drivetrain.
     * @param gyroAngle The current gyro angle.
     * @param wheelPositions The initial differential wheel positions.
     * @param initialPoseMeters The starting pose estimate.
     */
    public DifferentialDrivePoseEstimator(
            DifferentialDriveKinematics kinematics,
            Rotation2d gyroAngle,
            DifferentialDriveWheelPositions wheelPositions,
            Pose2d initialPoseMeters) {
        m_odometry = new DifferentialDriveOdometry(kinematics, gyroAngle, wheelPositions, initialPoseMeters);
        m_estimatedPose = initialPoseMeters;
        
        m_poseBuffer = TimeInterpolatableBuffer.createBuffer(75);
    }

    public void setVisionMeasurementStdDevs(double[] visionStdDevs) {
        if (visionStdDevs.length != 3) {
            throw new IllegalArgumentException("Standard deviations array must be of length 3");
        }
        m_visionStdDevs = visionStdDevs;
    }

    public void resetPosition(Rotation2d gyroAngle, DifferentialDriveWheelPositions wheelPositions, Pose2d poseMeters) {
        m_odometry.resetPosition(gyroAngle, wheelPositions, poseMeters);
        m_estimatedPose = poseMeters;
        m_poseBuffer.clear();
    }

    public Pose2d getEstimatedPosition() {
        return m_estimatedPose;
    }

    /**
     * Updates the pose estimator with exactly tracked differential wheel positions.
     *
     * @param gyroAngle The current gyro angle.
     * @param wheelPositions The current wheel positions.
     * @param timestampSeconds the exact time the modules were sampled.
     * @return The updated estimated pose.
     */
    public Pose2d update(Rotation2d gyroAngle, DifferentialDriveWheelPositions wheelPositions, double timestampSeconds) {
        m_estimatedPose = m_odometry.update(gyroAngle, wheelPositions);
        m_poseBuffer.addSample(timestampSeconds, m_estimatedPose);
        return m_estimatedPose;
    }

    /**
     * Adds a vision measurement to the pose estimator.
     * Rolls back Odometry in time, applying the vision measurement when the photo was literally taken,
     * and replays forward.
     * 
     * @param visionRobotPoseMeters The pose of the robot as measured by the vision camera.
     * @param timestampSeconds The precise time the photo was taken (e.g. current_time - pipeline_latency).
     */
    public void addVisionMeasurement(Pose2d visionRobotPoseMeters, double timestampSeconds) {
        Pose2d sample = m_poseBuffer.getSample(timestampSeconds);
        
        if (sample == null) {
            return; // Measurement is too old or buffer empty.
        }

        // Calculate the difference between what Odometry THOUGHT we were at t, and the Camera's read at t.
        Pose2d transform = visionRobotPoseMeters.relativeTo(sample);
        
        double xError = transform.getX();
        double yError = transform.getY();
        double thetaError = transform.getRotation().getRadians();
        
        double kX = 1.0 / (1.0 + m_visionStdDevs[0]);
        double kY = 1.0 / (1.0 + m_visionStdDevs[1]);
        double kTheta = 1.0 / (1.0 + m_visionStdDevs[2]);

        Pose2d correctedRetroPose = new Pose2d(
            sample.getX() + xError * kX,
            sample.getY() + yError * kY,
            new Rotation2d(sample.getRotation().getRadians() + thetaError * kTheta)
        );

        // Apply that same geometric offset to our CURRENT active position to replay the history!
        // We use rigorous Twist mathematics to append curve deviations safely.
        Twist2d replayTwist = sample.log(m_estimatedPose);
        m_estimatedPose = correctedRetroPose.exp(replayTwist);
        
        m_odometry.resetPosition(m_odometry.getPose().getRotation(), new DifferentialDriveWheelPositions(0,0), m_estimatedPose); 
    }
}

package org.areslib.core.localization;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.TimeInterpolatableBuffer;
import com.pedropathing.geometry.Pose;

/**
 * An advanced Pose Estimator that handles Vision Latency Compensation.
 * 
 * Works by maintaining a historical buffer of odometry poses. When a delayed vision
 * measurement is received, the estimator looks back in time to the exact moment
 * the picture was taken, calculates the vision error at that specific moment,
 * and then mathematically applies that correction to the current, modern pose.
 */
public class AresPoseEstimator {
    private final TimeInterpolatableBuffer<Pose2d> m_poseBuffer;
    private Pose2d m_currentOdometryPose = new Pose2d();
    private Pose2d m_visionOffset = new Pose2d();

    /**
     * @param historySize The number of samples to keep in the buffer (e.g. 50 for 1 second at 50hz).
     */
    public AresPoseEstimator(int historySize) {
        m_poseBuffer = TimeInterpolatableBuffer.createBuffer(historySize);
    }

    /**
     * Update the estimator with the latest Odometry (Pedro Pathing or Hardware) measurement.
     * This should be called every loop.
     * 
     * @param currentTimeSeconds The timestamp of this measurement.
     * @param odometryPose The uncorrected, pure odometry pose.
     */
    public void update(double currentTimeSeconds, Pose2d odometryPose) {
        m_currentOdometryPose = odometryPose;
        m_poseBuffer.addSample(currentTimeSeconds, odometryPose);
    }

    /**
     * Optional method to easily convert Pedro Pose to ARESLib Pose2d.
     */
    public void update(double currentTimeSeconds, Pose pedroPose) {
        update(currentTimeSeconds, new Pose2d(pedroPose.getX(), pedroPose.getY(), new Rotation2d(pedroPose.getHeading())));
    }

    /**
     * Adds a vision measurement to the estimator, automatically compensating for latency.
     * 
     * @param visionPose The global pose of the robot as seen by the camera.
     * @param timestampSeconds The timestamp of when the camera took the picture (CurrentTime - latency).
     * @param trustFactor [0.0 - 1.0] How much to trust this vision measurement (0.0 = ignore, 1.0 = instant override).
     */
    public void addVisionMeasurement(Pose2d visionPose, double timestampSeconds, double trustFactor) {
        if (trustFactor <= 0.0) return;
        
        Pose2d historicalOdometryPose = m_poseBuffer.getSample(timestampSeconds);
        
        if (historicalOdometryPose == null) {
            // Timestamp is too old or buffer is empty, ignore to prevent wild snaps
            return;
        }

        // What did the odometry think the position was when the picture was taken?
        // Add our current vision offset to that historical odometry pose to see our 
        // "estimated historical pose".
        Pose2d estimatedHistoricalPose = historicalOdometryPose.plus(m_visionOffset);

        // Find the GLOBAL Cartesian error between the Vision's answer and our Estimated answer at that time
        double globalXError = visionPose.getX() - estimatedHistoricalPose.getX();
        double globalYError = visionPose.getY() - estimatedHistoricalPose.getY();
        double thetaError = visionPose.getRotation().minus(estimatedHistoricalPose.getRotation()).getRadians();

        // Apply trust factor to scale the magnitude of the global shift
        double scaledX = globalXError * trustFactor;
        double scaledY = globalYError * trustFactor;
        double scaledTheta = thetaError * trustFactor;

        // Permanently add this global shift to our global vision offset
        m_visionOffset = new Pose2d(
            m_visionOffset.getX() + scaledX,
            m_visionOffset.getY() + scaledY,
            new Rotation2d(m_visionOffset.getRotation().getRadians() + scaledTheta)
        );
    }

    /**
     * @return The latency-compensated, latest estimated pose.
     */
    public Pose2d getEstimatedPosition() {
        return m_currentOdometryPose.plus(m_visionOffset);
    }

    /**
     * Resets the entire estimator to a specific position.
     * @param currentPose The pose to reset to.
     */
    public void resetPosition(Pose2d currentPose) {
        m_poseBuffer.clear();
        m_currentOdometryPose = currentPose;
        m_visionOffset = new Pose2d();
    }
}

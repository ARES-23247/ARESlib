package org.areslib.math.estimator;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.TimeInterpolatableBuffer;
import org.areslib.math.geometry.Twist2d;

/**
 * Shared latency-compensating vision fusion logic used by all Pose Estimators.
 * <p>
 * Performs the standard rollback-correct-replay algorithm:
 * <ol>
 *   <li>Look up the odometry pose at the vision measurement's timestamp.</li>
 *   <li>Compute the weighted correction offset based on standard deviations.</li>
 *   <li>Replay the odometry twist from the sample time to now on the corrected pose.</li>
 * </ol>
 */
public final class VisionFusionHelper {

    private VisionFusionHelper() {} // Utility class

    /**
     * Applies a vision measurement to the pose estimator's state.
     *
     * @param visionRobotPoseMeters The vision-measured robot pose.
     * @param timestampSeconds      The timestamp of the vision measurement.
     * @param currentEstimate       The current estimated pose (latest).
     * @param poseBuffer            The time-interpolatable pose history buffer.
     * @param visionStdDevs         Standard deviation weights [x, y, theta].
     * @return The corrected estimated pose, or {@code currentEstimate} if the measurement is too old.
     */
    public static Pose2d applyVisionMeasurement(
            Pose2d visionRobotPoseMeters,
            double timestampSeconds,
            Pose2d currentEstimate,
            TimeInterpolatableBuffer<Pose2d> poseBuffer,
            double[] visionStdDevs) {

        Pose2d sample = poseBuffer.getSample(timestampSeconds);

        if (sample == null) {
            return currentEstimate; // Measurement is too old or buffer empty.
        }

        // Calculate the difference between what Odometry thought and what Vision measured at t.
        Pose2d transform = visionRobotPoseMeters.relativeTo(sample);

        double xError = transform.getX();
        double yError = transform.getY();
        double thetaError = transform.getRotation().getRadians();

        // Dampen the correction using configured trust values
        double kX = 1.0 / (1.0 + visionStdDevs[0]);
        double kY = 1.0 / (1.0 + visionStdDevs[1]);
        double kTheta = 1.0 / (1.0 + visionStdDevs[2]);

        // Synthesize the corrected pose AT the measurement timestamp
        Pose2d correctedRetroPose = new Pose2d(
            sample.getX() + xError * kX,
            sample.getY() + yError * kY,
            new Rotation2d(sample.getRotation().getRadians() + thetaError * kTheta)
        );

        // Replay the exact odometry twist from sample time forward to now
        Twist2d replayTwist = sample.log(currentEstimate);
        return correctedRetroPose.exp(replayTwist);
    }
}

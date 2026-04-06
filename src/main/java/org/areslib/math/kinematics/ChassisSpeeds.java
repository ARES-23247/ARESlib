package org.areslib.math.kinematics;

import org.areslib.math.geometry.Rotation2d;

/**
 * Represents the speed of a robot chassis.
 */
public class ChassisSpeeds {
    public double vxMetersPerSecond;
    public double vyMetersPerSecond;
    public double omegaRadiansPerSecond;

    public ChassisSpeeds() {
    }

    public ChassisSpeeds(double vxMetersPerSecond, double vyMetersPerSecond, double omegaRadiansPerSecond) {
        this.vxMetersPerSecond = vxMetersPerSecond;
        this.vyMetersPerSecond = vyMetersPerSecond;
        this.omegaRadiansPerSecond = omegaRadiansPerSecond;
    }

    public static ChassisSpeeds fromFieldRelativeSpeeds(
            double vxMetersPerSecond,
            double vyMetersPerSecond,
            double omegaRadiansPerSecond,
            Rotation2d robotAngle) {
        return new ChassisSpeeds(
            vxMetersPerSecond * robotAngle.getCos() + vyMetersPerSecond * robotAngle.getSin(),
            -vxMetersPerSecond * robotAngle.getSin() + vyMetersPerSecond * robotAngle.getCos(),
            omegaRadiansPerSecond
        );
    }

    /**
     * Discretizes a continuous-time chassis speed.
     * 
     * <p>This function resolves "Drive Skew" in swerve drives. When actualizing a continuous 
     * command, the robot will actually drive an arc over the duration of the control loop (dt). 
     * This function uses Pose Exponential integration to compute the exact discrete speeds required 
     * to land perfectly at the end of the arc.
     *
     * @param vxMetersPerSecond     Forward velocity.
     * @param vyMetersPerSecond     Sideways velocity.
     * @param omegaRadiansPerSecond Angular velocity.
     * @param dtSeconds            The duration of the control loop (standard is 0.02).
     * @return Discretized ChassisSpeeds.
     */
    public static ChassisSpeeds discretize(
            double vxMetersPerSecond,
            double vyMetersPerSecond,
            double omegaRadiansPerSecond,
            double dtSeconds) {
        
        // Use the exact pose exponential mapping to calculate the arc displacement
        org.areslib.math.geometry.Pose2d desiredDeltaPose = new org.areslib.math.geometry.Pose2d(0, 0, new Rotation2d()).exp(
                new org.areslib.math.geometry.Twist2d(
                        vxMetersPerSecond * dtSeconds,
                        vyMetersPerSecond * dtSeconds,
                        omegaRadiansPerSecond * dtSeconds));
                        
        return new ChassisSpeeds(
                desiredDeltaPose.getX() / dtSeconds,
                desiredDeltaPose.getY() / dtSeconds,
                desiredDeltaPose.getRotation().getRadians() / dtSeconds);
    }

    public static ChassisSpeeds discretize(ChassisSpeeds continuousSpeeds, double dtSeconds) {
        return discretize(
                continuousSpeeds.vxMetersPerSecond,
                continuousSpeeds.vyMetersPerSecond,
                continuousSpeeds.omegaRadiansPerSecond,
                dtSeconds);
    }
    
    @Override
    public String toString() {
        return String.format(
            "ChassisSpeeds(Vx: %.2f m/s, Vy: %.2f m/s, Omega: %.2f rad/s)",
            vxMetersPerSecond, vyMetersPerSecond, omegaRadiansPerSecond
        );
    }
}

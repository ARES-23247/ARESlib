package org.areslib.math.kinematics;

import org.areslib.math.geometry.Translation2d;

public class MecanumDriveKinematics {
    private final double[][] m_inverseKinematics;
    private final double[][] m_forwardKinematics;

    public MecanumDriveKinematics(
            Translation2d frontLeftWheelMeters,
            Translation2d frontRightWheelMeters,
            Translation2d rearLeftWheelMeters,
            Translation2d rearRightWheelMeters) {

        m_inverseKinematics = new double[4][3];
        m_inverseKinematics[0][0] = 1; m_inverseKinematics[0][1] = -1; m_inverseKinematics[0][2] = -(frontLeftWheelMeters.getX() - frontLeftWheelMeters.getY());
        m_inverseKinematics[1][0] = 1; m_inverseKinematics[1][1] = 1;  m_inverseKinematics[1][2] = -(frontRightWheelMeters.getX() + frontRightWheelMeters.getY());
        m_inverseKinematics[2][0] = 1; m_inverseKinematics[2][1] = -1; m_inverseKinematics[2][2] = -(rearLeftWheelMeters.getX() - rearLeftWheelMeters.getY());
        m_inverseKinematics[3][0] = 1; m_inverseKinematics[3][1] = 1;  m_inverseKinematics[3][2] = -(rearRightWheelMeters.getX() + rearRightWheelMeters.getY());

        m_forwardKinematics = InverseMatrixHelper.pseudoInverse(m_inverseKinematics);
    }

    public MecanumDriveWheelSpeeds toWheelSpeeds(ChassisSpeeds chassisSpeeds) {
        return new MecanumDriveWheelSpeeds(
            chassisSpeeds.vxMetersPerSecond * m_inverseKinematics[0][0] + chassisSpeeds.vyMetersPerSecond * m_inverseKinematics[0][1] + chassisSpeeds.omegaRadiansPerSecond * m_inverseKinematics[0][2],
            chassisSpeeds.vxMetersPerSecond * m_inverseKinematics[1][0] + chassisSpeeds.vyMetersPerSecond * m_inverseKinematics[1][1] + chassisSpeeds.omegaRadiansPerSecond * m_inverseKinematics[1][2],
            chassisSpeeds.vxMetersPerSecond * m_inverseKinematics[2][0] + chassisSpeeds.vyMetersPerSecond * m_inverseKinematics[2][1] + chassisSpeeds.omegaRadiansPerSecond * m_inverseKinematics[2][2],
            chassisSpeeds.vxMetersPerSecond * m_inverseKinematics[3][0] + chassisSpeeds.vyMetersPerSecond * m_inverseKinematics[3][1] + chassisSpeeds.omegaRadiansPerSecond * m_inverseKinematics[3][2]
        );
    }

    public ChassisSpeeds toChassisSpeeds(MecanumDriveWheelSpeeds wheelSpeeds) {
        double vx = m_forwardKinematics[0][0] * wheelSpeeds.frontLeftMetersPerSecond +
                    m_forwardKinematics[0][1] * wheelSpeeds.frontRightMetersPerSecond +
                    m_forwardKinematics[0][2] * wheelSpeeds.rearLeftMetersPerSecond +
                    m_forwardKinematics[0][3] * wheelSpeeds.rearRightMetersPerSecond;

        double vy = m_forwardKinematics[1][0] * wheelSpeeds.frontLeftMetersPerSecond +
                    m_forwardKinematics[1][1] * wheelSpeeds.frontRightMetersPerSecond +
                    m_forwardKinematics[1][2] * wheelSpeeds.rearLeftMetersPerSecond +
                    m_forwardKinematics[1][3] * wheelSpeeds.rearRightMetersPerSecond;

        double omega = m_forwardKinematics[2][0] * wheelSpeeds.frontLeftMetersPerSecond +
                       m_forwardKinematics[2][1] * wheelSpeeds.frontRightMetersPerSecond +
                       m_forwardKinematics[2][2] * wheelSpeeds.rearLeftMetersPerSecond +
                       m_forwardKinematics[2][3] * wheelSpeeds.rearRightMetersPerSecond;

        return new ChassisSpeeds(vx, vy, omega);
    }

    /**
     * Converts an array of mecanum wheel position deltas into a single Twist2d delta.
     *
     * @param start Wheel positions at the start of the interval.
     * @param end Wheel positions at the end of the interval.
     * @return The twist over the interval.
     */
    public org.areslib.math.geometry.Twist2d toTwist2d(MecanumDriveWheelPositions start, MecanumDriveWheelPositions end) {
        MecanumDriveWheelSpeeds deltas = new MecanumDriveWheelSpeeds(
            end.frontLeftMeters - start.frontLeftMeters,
            end.frontRightMeters - start.frontRightMeters,
            end.rearLeftMeters - start.rearLeftMeters,
            end.rearRightMeters - start.rearRightMeters
        );
        ChassisSpeeds twistSpeeds = toChassisSpeeds(deltas);
        return new org.areslib.math.geometry.Twist2d(twistSpeeds.vxMetersPerSecond, twistSpeeds.vyMetersPerSecond, twistSpeeds.omegaRadiansPerSecond);
    }
}

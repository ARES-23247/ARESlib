package org.areslib.math.kinematics;

import org.areslib.math.geometry.Translation2d;
import org.areslib.math.geometry.Rotation2d;

/**
 * Helper class that converts a chassis velocity (dx, dy, and dtheta components) into individual
 * module states and vice versa.
 */
public class SwerveDriveKinematics {
    private final Translation2d[] m_modules;
    private final double[][] m_inverseKinematics;
    private final double[][] m_forwardKinematics;

    public SwerveDriveKinematics(Translation2d... moduleTranslations) {
        if (moduleTranslations.length < 2) {
            throw new IllegalArgumentException("A swerve drive requires at least two modules");
        }
        m_modules = moduleTranslations;
        int numModules = m_modules.length;

        m_inverseKinematics = new double[numModules * 2][3];
        for (int i = 0; i < numModules; i++) {
            m_inverseKinematics[i * 2][0] = 1;
            m_inverseKinematics[i * 2][1] = 0;
            m_inverseKinematics[i * 2][2] = -m_modules[i].getY();
            m_inverseKinematics[i * 2 + 1][0] = 0;
            m_inverseKinematics[i * 2 + 1][1] = 1;
            m_inverseKinematics[i * 2 + 1][2] = m_modules[i].getX();
        }

        m_forwardKinematics = InverseMatrixHelper.pseudoInverse(m_inverseKinematics);
    }

    /**
     * Converts a chassis speed to array of swerve module states.
     * 
     * @param chassisSpeeds The chassis speeds.
     * @return Array of swerve module states.
     */
    public SwerveModuleState[] toSwerveModuleStates(ChassisSpeeds chassisSpeeds) {
        SwerveModuleState[] states = new SwerveModuleState[m_modules.length];
        for (int i = 0; i < m_modules.length; i++) {
            double vx = chassisSpeeds.vxMetersPerSecond * m_inverseKinematics[i * 2][0] +
                        chassisSpeeds.vyMetersPerSecond * m_inverseKinematics[i * 2][1] +
                        chassisSpeeds.omegaRadiansPerSecond * m_inverseKinematics[i * 2][2];
            
            double vy = chassisSpeeds.vxMetersPerSecond * m_inverseKinematics[i * 2 + 1][0] +
                        chassisSpeeds.vyMetersPerSecond * m_inverseKinematics[i * 2 + 1][1] +
                        chassisSpeeds.omegaRadiansPerSecond * m_inverseKinematics[i * 2 + 1][2];
            
            states[i] = new SwerveModuleState(Math.hypot(vx, vy), new Rotation2d(vx, vy));
        }
        return states;
    }

    /**
     * Converts an array of swerve module states into a single chassis speed.
     *
     * @param moduleStates Array of swerve module states.
     * @return The chassis speed.
     */
    public ChassisSpeeds toChassisSpeeds(SwerveModuleState... moduleStates) {
        if (moduleStates.length != m_modules.length) {
            throw new IllegalArgumentException("Number of module states must match number of modules");
        }
        
        double vx = 0;
        double vy = 0;
        double omega = 0;
        
        for (int i = 0; i < m_modules.length; i++) {
            double moduleVx = moduleStates[i].speedMetersPerSecond * moduleStates[i].angle.getCos();
            double moduleVy = moduleStates[i].speedMetersPerSecond * moduleStates[i].angle.getSin();
            
            vx += m_forwardKinematics[0][i * 2] * moduleVx + m_forwardKinematics[0][i * 2 + 1] * moduleVy;
            vy += m_forwardKinematics[1][i * 2] * moduleVx + m_forwardKinematics[1][i * 2 + 1] * moduleVy;
            omega += m_forwardKinematics[2][i * 2] * moduleVx + m_forwardKinematics[2][i * 2 + 1] * moduleVy;
        }
        
        return new ChassisSpeeds(vx, vy, omega);
    }

    /**
     * Renormalizes the wheel speeds if any individual speed is above the specified maximum.
     *
     * @param moduleStates The array of module states.
     * @param attainableMaxSpeedMetersPerSecond The absolute max speed that a module can reach.
     */
    public static void desaturateWheelSpeeds(
            SwerveModuleState[] moduleStates, double attainableMaxSpeedMetersPerSecond) {
        double realMaxSpeed = 0.0;
        for (SwerveModuleState state : moduleStates) {
            realMaxSpeed = Math.max(realMaxSpeed, Math.abs(state.speedMetersPerSecond));
        }
        if (realMaxSpeed > attainableMaxSpeedMetersPerSecond) {
            for (SwerveModuleState state : moduleStates) {
                state.speedMetersPerSecond = state.speedMetersPerSecond / realMaxSpeed * attainableMaxSpeedMetersPerSecond;
            }
        }
    }

    /**
     * Converts an array of swerve module position deltas into a single Twist2d delta.
     *
     * @param start Module positions at the start of the interval.
     * @param end Module positions at the end of the interval.
     * @return The twist over the interval.
     */
    public org.areslib.math.geometry.Twist2d toTwist2d(SwerveModulePosition[] start, SwerveModulePosition[] end) {
        if (start.length != m_modules.length || end.length != m_modules.length) {
            throw new IllegalArgumentException("Number of module positions must match number of modules");
        }

        SwerveModuleState[] moduleDeltas = new SwerveModuleState[m_modules.length];
        for (int i = 0; i < m_modules.length; i++) {
            moduleDeltas[i] = new SwerveModuleState(
                end[i].distanceMeters - start[i].distanceMeters,
                end[i].angle
            );
        }

        ChassisSpeeds twistSpeeds = toChassisSpeeds(moduleDeltas);
        return new org.areslib.math.geometry.Twist2d(twistSpeeds.vxMetersPerSecond, twistSpeeds.vyMetersPerSecond, twistSpeeds.omegaRadiansPerSecond);
    }
}

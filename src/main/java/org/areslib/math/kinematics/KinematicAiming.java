package org.areslib.math.kinematics;

import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;

/**
 * Utility for "Shoot-On-The-Move" kinematic aiming.
 * <p>
 * Calculates the required lead angle to hit a static target while the robot is moving,
 * by factoring in the robot's current field-relative velocity and the projectile's expected
 * muzzle velocity. This allows a robot to confidently hit targets while strafing at high speeds.
 */
public class KinematicAiming {

    /**
     * The result of a kinematic aiming calculation.
     */
    public static class AimResult {
        /** The necessary robot heading yaw to hit the target. */
        public final Rotation2d requiredHeading;
        /** The virtual target translation (the point in space the robot should functionally aim at). */
        public final Translation2d virtualTarget;
        /** Estimated time of flight in seconds. */
        public final double timeOfFlight;
        /** True if a valid solution was found; false if the shot is mathematically impossible. */
        public final boolean isValid;

        public AimResult(Rotation2d requiredHeading, Translation2d virtualTarget, double timeOfFlight, boolean isValid) {
            this.requiredHeading = requiredHeading;
            this.virtualTarget = virtualTarget;
            this.timeOfFlight = timeOfFlight;
            this.isValid = isValid;
        }
    }

    /**
     * Calculates the required aiming parameters to hit a static target while moving.
     *
     * @param robotVelocityFieldRelative The current velocity of the robot relative to the field.
     *                                   (e.g., from {@link ChassisSpeeds} with robot heading).
     * @param robotPosition            The current position of the robot.
     * @param targetPosition           The position of the target.
     * @param projectileSpeedMetersPerSec The muzzle velocity of the projectile.
     * @return The AimResult containing the required heading and virtual target.
     */
    public static AimResult calculateAim(
            ChassisSpeeds robotVelocityFieldRelative,
            Translation2d robotPosition,
            Translation2d targetPosition,
            double projectileSpeedMetersPerSec) {

        // Relative distance vector D from robot to target
        Translation2d distanceVec = targetPosition.minus(robotPosition);
        double dx = distanceVec.getX();
        double dy = distanceVec.getY();

        // Robot velocity components
        double vx = robotVelocityFieldRelative.vxMetersPerSecond;
        double vy = robotVelocityFieldRelative.vyMetersPerSecond;

        // Solve quadratic equation for time of flight (t):
        // (s_p^2 - v_r^2) t^2 + 2(D · v_r)t - |D|^2 = 0
        
        double v_r_sq = vx * vx + vy * vy;
        double s_p_sq = projectileSpeedMetersPerSec * projectileSpeedMetersPerSec;
        double a = s_p_sq - v_r_sq;
        
        double dotProduct = dx * vx + dy * vy;
        double b = 2.0 * dotProduct;
        
        double d_sq = dx * dx + dy * dy;
        double c = -d_sq;

        if (Math.abs(a) < 1e-6) {
            // Projectile speed is exactly equal to robot speed. Linear fallback.
            if (Math.abs(b) < 1e-6) {
                return noValidSolution(distanceVec, robotPosition);
            }
            double t = -c / b;
            if (t > 0) {
                return buildResult(t, distanceVec, vx, vy, robotPosition);
            }
            return noValidSolution(distanceVec, robotPosition);
        }

        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            // No real roots -> target is unreachable at this speed
            return noValidSolution(distanceVec, robotPosition);
        }

        // Find the smallest positive root for time of flight
        double sqrtDisc = Math.sqrt(discriminant);
        double t1 = (-b + sqrtDisc) / (2 * a);
        double t2 = (-b - sqrtDisc) / (2 * a);

        double t = -1;
        if (t1 > 0 && t2 > 0) {
            t = Math.min(t1, t2);
        } else if (t1 > 0) {
            t = t1;
        } else if (t2 > 0) {
            t = t2;
        }

        if (t <= 0) {
            // Impossible to hit in the future
            return noValidSolution(distanceVec, robotPosition);
        }

        return buildResult(t, distanceVec, vx, vy, robotPosition);
    }

    private static AimResult buildResult(double t, Translation2d distanceVec, double vx, double vy, Translation2d robotPos) {
        // The point we need to aim at is where the target *will be* relative to the robot's frame if the robot were stationary.
        // Virtual target = Actual Target - (Robot Velocity * Time of Flight)
        double virtualX = distanceVec.getX() - (vx * t);
        double virtualY = distanceVec.getY() - (vy * t);
        
        Translation2d virtualVec = new Translation2d(virtualX, virtualY);
        Rotation2d requiredYaw = new Rotation2d(virtualX, virtualY);
        
        // Offset back to world coordinates for the virtual target output
        Translation2d virtualWorldTarget = robotPos.plus(virtualVec);

        return new AimResult(requiredYaw, virtualWorldTarget, t, true);
    }

    private static AimResult noValidSolution(Translation2d distanceVec, Translation2d robotPos) {
        // Fallback: aim directly at the true target
        Rotation2d directYaw = new Rotation2d(distanceVec.getX(), distanceVec.getY());
        return new AimResult(directYaw, robotPos.plus(distanceVec), 0.0, false);
    }
}

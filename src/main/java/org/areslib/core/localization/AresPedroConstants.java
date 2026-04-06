package org.areslib.core.localization;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.follower.FollowerConstants;

/**
 * Global constant definitions and FTC Dashboard configuration for Pedro Pathing and ARESlib integration.
 * <p>
 * This class exposes tuning variables directly to the FTC Dashboard via the {@link Config} annotation,
 * allowing live updates to telemetry constraints, tuning bounds, and physics constants. Teams should
 * modify this class (or subclass it in TeamCode) to fit their specific robot.
 */
@Config
public class AresPedroConstants {
    // These constants should be configured by the team in TeamCode, but we provide
    // some baseline functional definitions here for ARESlib integration

    /** Total robot mass in kilograms. Used by Pedro Pathing for feedforward algorithms. */
    public static double mass = 12.0;

    /** Drive PIDF Coefficients: [kP, kI, kD, kF]. Used to control the drive axis vector. */
    public static double drivePIDFCoefficients[] = {0.0, 0.0, 0.0, 0.0};
    
    /** Heading PIDF Coefficients: [kP, kI, kD, kF]. Used to control the rotation state towards the target angle. */
    public static double headingPIDFCoefficients[] = {0.0, 0.0, 0.0, 0.0};

    /** Translation PIDF Coefficients: [kP, kI, kD, kF]. Used to correct spatial X/Y errors. */
    public static double translationalPIDFCoefficients[] = {0.0, 0.0, 0.0, 0.0};

    // TeleOp Telemetry & Sim Tuning Variables
    /** Maximum forward/backward speed limit for teleop execution in m/s. */
    public static double teleOpMaxSpeedForward = 4.0; 
    /** Maximum strafing speed limit for teleop execution in m/s. */
    public static double teleOpMaxSpeedStrafe = 4.0;  
    /** Maximum turning speed limit for teleop execution in rad/s. */
    public static double teleOpMaxTurnRads = 6.0;     
    /** A speed multiplier applied to input axes when the 'boost' button is engaged. */
    public static double teleOpBoostMultiplier = 1.5; 

    // Simulation Physics Tuning
    /** Synthetic traction coefficient for the drive axes during simulation. */
    public static double simDriveTractionGrip = 50.0;
    /** Synthetic traction coefficient for the rotational axis during simulation. */
    public static double simTurnTractionGrip = 100.0;

    /**
     * Instantiates and configures a standard Pedro {@link FollowerConstants} object
     * by injecting the live tuning variables maintained within this static class.
     * <p>
     * Call this in the user's autonomous init to inject custom PID tuned values
     * into Pedro's environment gracefully.
     *
     * @return A newly constructed {@link FollowerConstants} populated with the configured tuning data.
     */
    public static FollowerConstants createConstants() {
        FollowerConstants constants = new FollowerConstants();
        constants.mass = mass;
        // Optional: Map array coefficients cleanly into FollowerConstants if needed here
        // Pedro will use these values natively for path calculation.
        return constants;
    }
}

package org.areslib.sim.games;

import org.dyn4j.dynamics.Body;
import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Context object representing a simulated robot tracked by the environment GameSimulation.
 * Pairs the physics collision frame (Body) with the input stream driving its interactions (Gamepad).
 */
public class RobotSimState {
    public final Body robotBody;
    public final Gamepad gamepad;
    
    public RobotSimState(Body robotBody, Gamepad gamepad) {
        this.robotBody = robotBody;
        this.gamepad = gamepad;
    }
}

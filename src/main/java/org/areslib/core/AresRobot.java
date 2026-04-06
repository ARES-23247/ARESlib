package org.areslib.core;

/**
 * Global utility class representing robot state.
 */
public class AresRobot {
    private static boolean isSimulation = false;

    /**
     * @return True if the robot is currently running in a simulated environment.
     */
    public static boolean isSimulation() {
        return isSimulation;
    }

    /**
     * Sets the simulation state. This is typically invoked by desktop launchers like DesktopSimLauncher.
     * @param sim true if running in simulation
     */
    public static void setSimulation(boolean sim) {
        isSimulation = sim;
    }
}

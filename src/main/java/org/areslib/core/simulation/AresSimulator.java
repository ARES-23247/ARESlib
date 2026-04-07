package org.areslib.core.simulation;

import org.areslib.command.CommandScheduler;
import org.areslib.command.Subsystem;
import org.areslib.core.AresRobot;

/**
 * High-performance backend orchestrator for decoupled physics constraints.
 * Separates physical OpMode robot loops from Dyn4j spatial calculations.
 */
public class AresSimulator {
    private static Thread physicsThread = null;
    private static boolean isRunning = false;

    /**
     * Starts an independent background thread that executes subsystem.simulationPeriodic()
     * iteratively at a specified cycle rate.
     * 
     * @param periodMs The length of the targeted physics tick period in milliseconds (ex: 5ms for 200Hz).
     */
    public static synchronized void startPhysicsSim(int periodMs) {
        // Prevent running if not in a simulated environment
        if (!AresRobot.isSimulation()) {
            System.err.println("AresSimulator warning: Ignoring start command; AresRobot is not in simulated mode.");
            return;
        }

        if (isRunning) return;
        isRunning = true;

        // Populate the physical world with DECODE assets
        DecodeFieldSim.buildField();

        physicsThread = new Thread(() -> {
            while (isRunning && AresRobot.isSimulation()) {
                long start = System.currentTimeMillis();

                // Step the centralized physics world
                double dtSeconds = periodMs / 1000.0;
                AresPhysicsWorld.getInstance().step(dtSeconds);

                // Poll physics logic decoupled from main scheduling pipeline
                for (Subsystem subsystem : CommandScheduler.getInstance().getSubsystems()) {
                    subsystem.simulationPeriodic();
                }

                long elapsed = System.currentTimeMillis() - start;
                long sleepTime = periodMs - elapsed;

                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        
        // Mark as daemon so it dies implicitly alongside the main application thread
        physicsThread.setDaemon(true);
        // Ensure simulation engine evaluates prior to standard background operations
        physicsThread.setPriority(Thread.MAX_PRIORITY);
        physicsThread.start();
    }

    /**
     * Halts the background physics engine processing.
     */
    public static synchronized void stopPhysicsSim() {
        isRunning = false;
        if (physicsThread != null) {
            physicsThread.interrupt();
            physicsThread = null;
        }
    }
}

package org.areslib.core;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.hardware.lynx.LynxModule;

import org.areslib.command.CommandScheduler;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.telemetry.AresTelemetry;

import java.util.List;

/**
 * Base high-performance OpMode class.
 * Integrates PhotonCore for thread lock bypassing and enables manual bulk caching.
 */
public abstract class AresCommandOpMode extends LinearOpMode {

    private List<LynxModule> allHubs;

    /**
     * Subclasses must override this to initialize their robots (subsystems, default commands).
     */
    public abstract void robotInit();

    public void runOpMode() throws InterruptedException {
        // Enable extensions automatically
        // pre-flight hooks go here.
        // Enable locally-vendored SolversLib PhotonCore for extreme I2C loop bypass efficiency
        try {
            org.areslib.hardware.coprocessors.photon.PhotonCore.experimental.setMaximumParallelCommands(8);
            org.areslib.hardware.coprocessors.photon.PhotonCore.experimental.setSinglethreadedOptimized(false);
            org.areslib.hardware.coprocessors.photon.PhotonCore.PARALLELIZE_SERVOS = true;
            org.areslib.hardware.coprocessors.photon.PhotonCore.enable();
        } catch (Exception ignored) {}
        
        allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }

        // Run user-provided initialization code
        robotInit();

        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            // 1. Clear bulk cache
            for (LynxModule hub : allHubs) {
                hub.clearBulkCache();
            }

            // 2. Clear Hardware Coprocessors (OctoQuad, SRS Hub) cache dynamically
            AresHardwareManager.clearCoprocessorCaches();

            // 3. Update Dual-Axis Governor for Voltage Comp & Load Shedding
            AresHardwareManager.updatePowerStatus();

            // 4. Command Scheduler Loop
            CommandScheduler.getInstance().run();

            // 5. Fire Telemetry
            AresTelemetry.update();
        }

        // OpMode finished, reset scheduler state for next run
        CommandScheduler.getInstance().cancelAll();
    }
}

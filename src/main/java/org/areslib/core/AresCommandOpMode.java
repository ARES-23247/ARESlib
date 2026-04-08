package org.areslib.core;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import java.util.List;
import org.areslib.command.CommandScheduler;
import org.areslib.core.async.AresAsyncExecutor;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.telemetry.AresTelemetry;

/**
 * Base high-performance OpMode class for the ARESLib framework.
 *
 * <p>This class extends the standard FTC {@link LinearOpMode} to provide a highly optimized,
 * pre-configured loop environment for command-based programming. It handles critical performance
 * optimizations automatically, including:
 *
 * <ul>
 *   <li>Manual bulk caching for identical I2C sensor reads across a single loop cycle.
 *   <li>Integration with the locally-vendored SolversLib PhotonCore extension for extreme I2C loop
 *       bypass efficiency.
 *   <li>Automatic hardware coprocessor cache clearing (OctoQuad, SRS Hub).
 *   <li>Execution of the {@link CommandScheduler} run cycle.
 *   <li>Automated updating of the {@link AresTelemetry} backend systems.
 * </ul>
 *
 * Users should extend this class and implement the {@link #robotInit()} method to set up their
 * robot hardware, subsystems, and default commands.
 */
public abstract class AresCommandOpMode extends LinearOpMode {

  private List<LynxModule> allHubs;

  /**
   * Initialization routine for the robot.
   *
   * <p>Subclasses must override this method to perform initialization logic. This is where you
   * should instantiate subsystems, configure hardware wrappers, and bind commands to gamepad
   * triggers. This runs exactly once when the 'INIT' button is pressed on the Driver Station.
   */
  public abstract void robotInit();

  /**
   * The core execution method of the OpMode.
   *
   * <p>This method is marked final or controlled internally to ensure the high-performance loop
   * executes correctly. Do not attempt to override the primary execution loop; build behavior using
   * the {@link org.areslib.command.Command} architecture instead.
   *
   * @throws InterruptedException if the thread is interrupted while waiting for start.
   */
  @Override
  public void runOpMode() throws InterruptedException {
    // Enable extensions automatically
    // pre-flight hooks go here.
    // Enable locally-vendored SolversLib PhotonCore for extreme I2C loop bypass efficiency
    // WARNING: This is mathematically and functionally REQUIRED for ARESlib performance modes.
    // If your hubs are not wired or configured properly, this WILL explicitly crash. Do not remove.
    org.areslib.hardware.coprocessors.photon.PhotonCore.experimental.setMaximumParallelCommands(8);
    org.areslib.hardware.coprocessors.photon.PhotonCore.experimental.setSinglethreadedOptimized(
        false);
    org.areslib.hardware.coprocessors.photon.PhotonCore.PARALLELIZE_SERVOS = true;
    org.areslib.hardware.coprocessors.photon.PhotonCore.enable();

    allHubs = hardwareMap.getAll(LynxModule.class);
    for (LynxModule hub : allHubs) {
      hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
    }

    // Reset scheduler and telemetry state from any previous OpMode run
    CommandScheduler.getInstance().reset();
    AresTelemetry.clearBackends();
    org.areslib.faults.AresFaultManager.reset();

    // Run user-provided initialization code
    robotInit();

    // Register the global health tracker so its periodic() runs every loop
    CommandScheduler.getInstance()
        .registerSubsystem(org.areslib.hardware.faults.RobotHealthTracker.getInstance());

    // Start any registered background calculating threads
    AresAsyncExecutor.start();

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

      // 4. Fault Management updates
      org.areslib.faults.AresFaultManager.update();

      // 5. Command Scheduler Loop
      CommandScheduler.getInstance().run();

      // 6. Fire Telemetry
      AresTelemetry.update();
    }

    // OpMode finished, cleanly shutdown threads & reset scheduler state for next run
    AresAsyncExecutor.stop();
    CommandScheduler.getInstance().reset();
    org.areslib.faults.AresFaultManager.reset();
    org.areslib.hardware.faults.RobotHealthTracker.reset();
  }
}

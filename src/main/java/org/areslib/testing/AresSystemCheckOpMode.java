package org.areslib.testing;

import org.areslib.command.Command;
import org.areslib.core.AresCommandOpMode;
import org.areslib.hmi.AresLEDManager;
import org.areslib.hmi.LEDState;

/**
 * Abstract base OpMode for running standardized pre-match hardware system checks.
 *
 * <p>In FRC, pit crews run a "SysCheck" to briefly actuate all mechanisms and verify encoder
 * integrity. ARESLib2 brings this to FTC.
 *
 * <p>By extending this OpMode, teams can define a sequential command that steps through their
 * assemblies (e.g. spin intake, check beam break, lift elevator, read encoder limit).
 */
public abstract class AresSystemCheckOpMode extends AresCommandOpMode {

  @Override
  public void robotInit() {
    // Initialize the Hardware and Subsystems configured in RobotContainer
    configureSubsystems();

    // Notify pit crew that we entered Diagnostic mode
    AresLEDManager.getInstance().init(hardwareMap, "blinkin");
    AresLEDManager.getInstance().setState(LEDState.DIAGNOSTIC);

    // Schedule the system check routine
    Command systemCheckRoutine = getSystemCheckCommand();
    if (systemCheckRoutine != null) {
      org.areslib.command.CommandScheduler.getInstance()
          .schedule(
              systemCheckRoutine.andThen(
                  new org.areslib.command.InstantCommand(
                      () -> {
                        if (AresLEDManager.getInstance().getCurrentState() == LEDState.DIAGNOSTIC) {
                          AresLEDManager.getInstance()
                              .setState(LEDState.HAS_GAME_PIECE); // Green = Good to Go
                        }
                      })));
    }
  }

  /** Implement this method to instantiate your robot's subsystems. */
  public abstract void configureSubsystems();

  /**
   * Define the sequential command routine that actuates hardware to verify health. Use commands
   * that verify encoder movement or switch states and emit faults if failed.
   *
   * @return The Command to schedule during initialization.
   */
  public abstract Command getSystemCheckCommand();
}

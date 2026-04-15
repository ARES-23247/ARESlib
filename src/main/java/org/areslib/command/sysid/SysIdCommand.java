package org.areslib.command.sysid;

import org.areslib.command.Command;
import org.areslib.telemetry.AresAutoLogger;

/**
 * The executing state machine that applies exact voltage over time and logs the mechanism's
 * instantaneous state in the official WPILib `SysId` format.
 */
public class SysIdCommand extends Command {

  private final SysIdRoutine.Config config;
  private final SysIdRoutine.Mechanism mechanism;
  private final boolean isQuasistatic;
  private final SysIdRoutine.Direction direction;
  private final String stateName;

  private double accumulator = 0.0;
  private double startTimeSeconds = 0.0;
  private SysIdJSONExporter.TestRecord currentTestRecord;

  /**
   * Constructs a new SysId test command.
   *
   * @param config The SysId configuration constraints.
   * @param mechanism The mechanism bindings allowing I/O access.
   * @param isQuasistatic True if the test is a slow voltage ramp (quasistatic). False for an
   *     instant step (dynamic).
   * @param direction The direction of the test.
   */
  public SysIdCommand(
      SysIdRoutine.Config config,
      SysIdRoutine.Mechanism mechanism,
      boolean isQuasistatic,
      SysIdRoutine.Direction direction) {
    this.config = config;
    this.mechanism = mechanism;
    this.isQuasistatic = isQuasistatic;
    this.direction = direction;

    stateName =
        (isQuasistatic ? "quasistatic" : "dynamic")
            + "-"
            + (direction == SysIdRoutine.Direction.FORWARD ? "forward" : "reverse");

    addRequirements(mechanism.requirements);
  }

  @Override
  public void initialize() {
    accumulator = 0.0;
    startTimeSeconds = org.areslib.core.AresTimer.getFPGATimestamp();
    AresAutoLogger.recordOutput("SysId/State", stateName);
    currentTestRecord = SysIdJSONExporter.startTest(stateName);
  }

  @Override
  public void execute() {
    accumulator = org.areslib.core.AresTimer.getFPGATimestamp() - startTimeSeconds;

    double volts;
    if (isQuasistatic) {
      volts = accumulator * config.rampRateVoltsPerSec;
    } else {
      volts = config.stepVoltageVolts;
    }
    volts *= direction.multiplier;

    mechanism.voltageInput.accept(volts);

    double pos = mechanism.positionOutput.get();
    double vel = mechanism.velocityOutput.get();

    // Push standardized keys to telemetry
    AresAutoLogger.recordOutput("SysId/Voltage", volts);
    AresAutoLogger.recordOutput("SysId/Position", pos);
    AresAutoLogger.recordOutput("SysId/Velocity", vel);

    // Feed to JSON local cache
    currentTestRecord.addFrame(accumulator, volts, pos, vel);
  }

  @Override
  public boolean isFinished() {
    return accumulator >= config.timeoutSeconds;
  }

  @Override
  public void end(boolean interrupted) {
    mechanism.voltageInput.accept(0.0);
    AresAutoLogger.recordOutput("SysId/State", "none");
    AresAutoLogger.recordOutput("SysId/Voltage", 0.0);
  }
}

package org.areslib.diagnostics;

import java.util.Arrays;
import org.areslib.command.Command;
import org.areslib.command.SequentialCommandGroup;
import org.areslib.faults.AresAlert;
import org.areslib.faults.AresAlert.AlertType;

/**
 * A comprehensive, robust pre-match diagnostic routine decoupled from mechanism logic.
 *
 * <p>Takes any number of {@link SystemTestable} objects and sequences them sequentially, starting
 * with a Battery Voltage Gate to ensure physical capability.
 *
 * <p>This ensures all robot hardware is functioning before match time, preventing mid-match
 * failures.
 */
public class SystemCheckCommand extends SequentialCommandGroup {

  private static final double MINIMUM_BATTERY_VOLTAGE = 12.0;
  private final AresAlert batteryAlert;
  private final AresAlert successAlert;
  private final Command[] tests;
  private int currentTestIndex = 0;
  private boolean initialized = false;

  /**
   * Safely iterates through all passed testable subsystems.
   *
   * @param subsystems Array or varargs of components implementing SystemTestable.
   */
  public SystemCheckCommand(SystemTestable... subsystems) {
    this.batteryAlert =
        new AresAlert("SystemCheck: Battery too low to test! Swap battery.", AlertType.ERROR);
    this.successAlert = new AresAlert("SystemCheck: ALL SYSTEM ROUTINES EXECUTED", AlertType.INFO);

    this.tests =
        Arrays.stream(subsystems)
            .map(SystemTestable::getSystemCheckCommand)
            .toArray(Command[]::new);
  }

  @Override
  public void initialize() {
    currentTestIndex = 0;
    initialized = true;

    // Check battery voltage first
    double voltage = getBatteryVoltage();
    if (voltage < MINIMUM_BATTERY_VOLTAGE) {
      batteryAlert.set(true);
      throw new IllegalStateException("Battery below 12.0V, aborting physical tests.");
    } else {
      batteryAlert.set(false);
      successAlert.set(false);
    }

    // Initialize first test
    if (tests.length > 0) {
      tests[0].initialize();
    }
  }

  @Override
  public void execute() {
    if (currentTestIndex >= tests.length) {
      return; // All tests complete
    }

    Command currentTest = tests[currentTestIndex];
    currentTest.execute();

    // If current test is finished, move to next
    if (currentTest.isFinished()) {
      currentTest.end(false);
      currentTestIndex++;

      // Initialize next test if available
      if (currentTestIndex < tests.length) {
        tests[currentTestIndex].initialize();
      } else {
        // All tests complete
        successAlert.set(true);
      }
    }
  }

  @Override
  public boolean isFinished() {
    return initialized && currentTestIndex >= tests.length;
  }

  @Override
  public void end(boolean interrupted) {
    // Clean up any running test
    if (currentTestIndex < tests.length && !tests[currentTestIndex].isFinished()) {
      tests[currentTestIndex].end(true);
    }

    if (interrupted) {
      new AresAlert("SystemCheck interrupted at test " + (currentTestIndex + 1), AlertType.WARNING)
          .set(true);
    }
  }

  /**
   * Get current battery voltage. Override this for custom voltage sources.
   *
   * @return Battery voltage in volts
   */
  protected double getBatteryVoltage() {
    // Default implementation - override in subclass for custom hardware
    return 13.0; // Placeholder return
  }

  /**
   * Get the number of tests in this system check.
   *
   * @return Number of tests
   */
  public int getTestCount() {
    return tests.length;
  }

  /**
   * Get the index of the currently running test.
   *
   * @return Current test index (0-based)
   */
  public int getCurrentTestIndex() {
    return currentTestIndex;
  }
}

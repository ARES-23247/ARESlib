package org.areslib.diagnostics;

import org.areslib.command.Command;

/**
 * Indicates that a subsystem or component can perform an automated self-test.
 *
 * <p>Implementing classes must provide a command that executes physical maneuvers and assert
 * hardware readings against strict tolerances, triggering Alerts upon failure.
 *
 * <p>This interface enables comprehensive pre-match diagnostics and automated hardware validation.
 */
public interface SystemTestable {
  /**
   * Generates an autonomous, state-machine style verification routine for this subsystem.
   *
   * @return A command that runs through diagnostic checks and logs results.
   */
  Command getSystemCheckCommand();
}

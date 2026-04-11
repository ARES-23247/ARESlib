package org.areslib.command;

import java.util.function.BooleanSupplier;

/**
 * A command that runs one of two commands depending on the evaluation of a given condition when
 * this command is initialized.
 */
public class ConditionalCommand extends Command {
  private final Command onTrue;
  private final Command onFalse;
  private final BooleanSupplier condition;
  private Command selectedCommand;

  /**
   * Creates a new ConditionalCommand.
   *
   * @param onTrue the command to run if the condition evaluates to true
   * @param onFalse the command to run if the condition evaluates to false
   * @param condition the condition to evaluate
   */
  public ConditionalCommand(Command onTrue, Command onFalse, BooleanSupplier condition) {
    this.onTrue = onTrue;
    this.onFalse = onFalse;
    this.condition = condition;

    requirements.addAll(onTrue.getRequirements());
    requirements.addAll(onFalse.getRequirements());
  }

  @Override
  public void initialize() {
    if (condition.getAsBoolean()) {
      selectedCommand = onTrue;
    } else {
      selectedCommand = onFalse;
    }
    selectedCommand.initialize();
  }

  @Override
  public void execute() {
    selectedCommand.execute();
  }

  @Override
  public void end(boolean interrupted) {
    selectedCommand.end(interrupted);
  }

  @Override
  public boolean isFinished() {
    return selectedCommand.isFinished();
  }
}

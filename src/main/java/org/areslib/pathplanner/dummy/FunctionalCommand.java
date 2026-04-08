package org.areslib.pathplanner.dummy;

/**
 * A dummy shim implementation to allow PathPlanner compilation without native WPILib/Android
 * dependencies.
 */
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.areslib.command.Command;
import org.areslib.command.Subsystem;

public class FunctionalCommand extends Command {
  private final Runnable onInit;
  private final Runnable onExecute;
  private final Consumer<Boolean> onEnd;
  private final BooleanSupplier isFinished;

  public FunctionalCommand(
      Runnable onInit,
      Runnable onExecute,
      Consumer<Boolean> onEnd,
      BooleanSupplier isFinished,
      Subsystem... requirements) {
    this.onInit = onInit;
    this.onExecute = onExecute;
    this.onEnd = onEnd;
    this.isFinished = isFinished;
    addRequirements(requirements);
  }

  @Override
  public void initialize() {
    onInit.run();
  }

  @Override
  public void execute() {
    onExecute.run();
  }

  @Override
  public boolean isFinished() {
    return isFinished.getAsBoolean();
  }

  @Override
  public void end(boolean interrupted) {
    onEnd.accept(interrupted);
  }
}

package org.areslib.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A state machine representing a complete action to be performed by the robot. Commands are run by
 * the CommandScheduler, and can be composed into CommandGroups to allow users to build complicated
 * multi-step actions without the need to write a state machine.
 */
public abstract class Command {
  protected Set<Subsystem> requirements = new HashSet<>();

  protected Command() {}

  /** The initial subroutine of a command. Called once when the command is initially scheduled. */
  public void initialize() {}

  /** The main body of a command. Called repeatedly while the command is scheduled. */
  public void execute() {}

  /**
   * The action to take when the command ends. Called when either the command finishes normally, or
   * when it interrupted.
   *
   * @param interrupted whether the command was interrupted/canceled
   */
  public void end(boolean interrupted) {}

  /**
   * Whether the command has finished. Once a command finishes, the scheduler will call its end()
   * method and un-schedule it.
   *
   * @return whether the command has finished.
   */
  public boolean isFinished() {
    return false;
  }

  /**
   * Specifies that the given subsystems are used by this command.
   *
   * @param requirements the subsystems the command requires
   */
  public final void addRequirements(Subsystem... requirements) {
    this.requirements.addAll(Arrays.asList(requirements));
  }

  /**
   * Gets the subsystems required by this command.
   *
   * @return the set of required subsystems
   */
  public Set<Subsystem> getRequirements() {
    return requirements;
  }

  /**
   * Decorates this command with a timeout. If the specified timeout is exceeded before the command
   * finishes normally, the command will be interrupted and un-scheduled.
   *
   * @param seconds the timeout in seconds
   * @return the decorated command
   */
  public Command withTimeout(double seconds) {
    return new ParallelRaceGroup(this, new WaitCommand(seconds));
  }

  /**
   * Decorates this command with a set of commands to run after it in sequence. Often more
   * convenient/less-verbose than constructing a new {@link SequentialCommandGroup} explicitly.
   *
   * @param next the commands to run next
   * @return the decorated command
   */
  public Command andThen(Command... next) {
    SequentialCommandGroup group = new SequentialCommandGroup(this);
    group.addCommands(next);
    return group;
  }

  /**
   * Decorates this command with a set of commands to run before it in sequence.
   *
   * @param before the commands to run before
   * @return the decorated command
   */
  public Command beforeStarting(Command... before) {
    SequentialCommandGroup group = new SequentialCommandGroup(before);
    group.addCommands(this);
    return group;
  }

  /**
   * Decorates this command with a set of commands to run concurrently with it, ending when all
   * commands have finished.
   *
   * @param parallel the commands to run in parallel
   * @return the decorated command
   */
  public Command alongWith(Command... parallel) {
    ParallelCommandGroup group = new ParallelCommandGroup(this);
    group.addCommands(parallel);
    return group;
  }

  /**
   * Decorates this command with a set of commands to run concurrently with it, ending when any
   * command finishes.
   *
   * @param parallel the commands to run in parallel
   * @return the decorated command
   */
  public Command raceWith(Command... parallel) {
    ParallelRaceGroup group = new ParallelRaceGroup(this);
    group.addCommands(parallel);
    return group;
  }

  /**
   * Decorates this command with a set of commands to run concurrently with it, ending when this
   * command finishes.
   *
   * @param parallel the commands to run in parallel
   * @return the decorated command
   */
  public Command deadlineWith(Command... parallel) {
    ParallelDeadlineGroup group = new ParallelDeadlineGroup(this);
    group.addCommands(parallel);
    return group;
  }
}

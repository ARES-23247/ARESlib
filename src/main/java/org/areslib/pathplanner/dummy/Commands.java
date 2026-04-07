package org.areslib.pathplanner.dummy;
import org.areslib.command.Command;
import org.areslib.command.InstantCommand;
import org.areslib.command.SequentialCommandGroup;

/**
 * A dummy implementation of the WPILib Commands factory.
 * This class translates WPILib-style command requests internally into ARESLib2's compatible
 * custom command framework to allow PathPlanner configuration mapping without android/WPILib runtime.
 */
public class Commands {
    
    /**
     * Creates a SequentialCommandGroup from the provided commands.
     * 
     * @param cmds array of commands to execute sequentially
     * @return the generated SequentialCommandGroup
     */
    public static Command sequence(Command... cmds) { return new SequentialCommandGroup(cmds); }

    /**
     * Executes one command or another depending on a condition evaluated at initialization time.
     * 
     * @param onTrue the command to execute if condition is true
     * @param onFalse the command to execute if condition is false
     * @param sel the boolean condition supplier
     * @return an InstantCommand that runs the chosen command's initialization
     */
    public static Command either(Command onTrue, Command onFalse, java.util.function.BooleanSupplier sel) {
        return new InstantCommand(() -> {
            if (sel.getAsBoolean()) {
                onTrue.initialize();
            } else {
                onFalse.initialize();
            }
        });
    }

    /**
     * Creates a command that runs the given runnable once and finishes immediately.
     * 
     * @param r the runnable to execute
     * @return the InstantCommand wrapping the runnable
     */
    public static Command runOnce(Runnable r) { return new InstantCommand(r); }

    /**
     * A basic delay command utilizing System.currentTimeMillis() for headless runtime compatibility.
     * 
     * @param s the number of seconds to wait
     * @return the delay Command
     */
    public static Command waitSeconds(double s) {
        return new Command() {
            private long startTime;
            @Override public void initialize() { startTime = System.currentTimeMillis(); }
            @Override public boolean isFinished() {
                return (System.currentTimeMillis() - startTime) >= (s * 1000.0);
            }
        };
    }

    /**
     * Returns a command that does absolutely nothing right away.
     * 
     * @return an empty InstantCommand
     */
    public static Command none() { return new InstantCommand(() -> {}); }

    /**
     * Returns a command that simply prints the provided string to standard output.
     * 
     * @param s string to print
     * @return the printing InstantCommand
     */
    public static Command print(String s) { return new InstantCommand(() -> System.out.println(s)); }

    /**
     * Creates a command that executes the provided runnable periodically and never finishes until interrupted.
     * 
     * @param r the runnable to execute every cycle
     * @return the generated continuous Command
     */
    public static Command run(Runnable r) {
        return new Command() {
            @Override public void execute() { r.run(); }
            @Override public boolean isFinished() { return false; }
        };
    }

    /**
     * Creates a command that builds the inner command dynamically at runtime rather than at definition time.
     * 
     * @param s the command supplier
     * @return the deferred command wrapper
     */
    public static Command defer(java.util.function.Supplier<Command> s) {
        return new Command() {
            private Command inner;
            @Override public void initialize() { inner = s.get(); if (inner != null) inner.initialize(); }
            @Override public void execute() { if (inner != null) inner.execute(); }
            @Override public boolean isFinished() { return inner == null || inner.isFinished(); }
            @Override public void end(boolean interrupted) { if (inner != null) inner.end(interrupted); }
        };
    }

    /**
     * Creates a ParallelDeadlineGroup running until the deadline command finishes.
     * 
     * @param deadline the command whose completion causes the group to end
     * @param cmds the other commands to run in parallel
     * @return the ParallelDeadlineGroup
     */
    public static Command deadline(Command deadline, Command... cmds) {
        return new org.areslib.command.ParallelDeadlineGroup(deadline, cmds);
    }
}
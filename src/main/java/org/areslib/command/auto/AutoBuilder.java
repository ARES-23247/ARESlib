package org.areslib.command.auto;

import org.areslib.command.Command;
import org.areslib.command.InstantCommand;
import org.areslib.command.ParallelDeadlineGroup;
import org.areslib.command.SequentialCommandGroup;
import org.areslib.command.WaitCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * A fluent builder for constructing autonomous command sequences.
 * <p>
 * Provides a declarative API for composing paths, actions, waits, and parallel groups
 * into a single {@link SequentialCommandGroup}. This is the generic base that accepts
 * any {@link Command} — see the standard AutoBuilder for convenience methods.
 *
 * <pre>{@code
 * Command auto = new AutoBuilder()
 *     .drive(driveToScoreCmd)
 *     .runAction(new ScoreCommand(arm))
 *     .waitSeconds(0.5)
 *     .drive(driveToStackCmd)
 *     .build();
 * }</pre>
 */
public class AutoBuilder {

    protected final List<Command> m_commands = new ArrayList<>();

    /**
     * Appends a driving command to the sequence.
     *
     * @param driveCommand Any command that moves the robot (e.g., FollowPathCommand).
     * @return This builder for chaining.
     */
    public AutoBuilder drive(Command driveCommand) {
        m_commands.add(driveCommand);
        return this;
    }

    /**
     * Appends an action command to the sequence (e.g., scoring, intake).
     *
     * @param action The action to execute.
     * @return This builder for chaining.
     */
    public AutoBuilder runAction(Command action) {
        m_commands.add(action);
        return this;
    }

    /**
     * Appends an instant action (runs once, no duration).
     *
     * @param action The runnable to execute instantly.
     * @return This builder for chaining.
     */
    public AutoBuilder runOnce(Runnable action) {
        m_commands.add(new InstantCommand(action));
        return this;
    }

    /**
     * Appends a wait period to the sequence.
     *
     * @param seconds The duration to wait in seconds.
     * @return This builder for chaining.
     */
    public AutoBuilder waitSeconds(double seconds) {
        m_commands.add(new WaitCommand(seconds));
        return this;
    }

    /**
     * Appends a parallel group where the deadline finishes the group.
     * <p>
     * The deadline command is typically a driving command, and the actions
     * run concurrently alongside it. When the deadline finishes, all other
     * commands are interrupted.
     *
     * @param deadline The command that determines when the group ends.
     * @param actions  Actions to run concurrently with the deadline.
     * @return This builder for chaining.
     */
    public AutoBuilder runParallel(Command deadline, Command... actions) {
        m_commands.add(new ParallelDeadlineGroup(deadline, actions));
        return this;
    }

    /**
     * Builds the autonomous routine as a composed sequential command.
     *
     * @return The complete autonomous command.
     */
    public Command build() {
        return new SequentialCommandGroup(m_commands.toArray(new Command[0]));
    }
}

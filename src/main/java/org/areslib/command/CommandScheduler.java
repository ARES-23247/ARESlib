package org.areslib.command;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The scheduler responsible for running Commands. A Command-based robot should call
 * {@link CommandScheduler#run()} in its periodic block in order to run commands synchronously.
 * 
 * Uses standard Java logic and handles generic subsystems and commands.
 */
public final class CommandScheduler {
    private static CommandScheduler instance;

    // A set of all registered subsystems
    private final Set<Subsystem> m_subsystems = new LinkedHashSet<>();
    // Subsystem default commands
    private final Map<Subsystem, Command> m_defaultCommands = new LinkedHashMap<>();
    
    // Commands currently executing
    private final Set<Command> m_scheduledCommands = new LinkedHashSet<>();
    // Subsystems required by currently executing commands
    private final Map<Subsystem, Command> m_requirements = new LinkedHashMap<>();

    // Buffer for commands scheduled during iteration
    private final java.util.List<Command> m_toSchedule = new java.util.ArrayList<>();
    // Buffer for commands cancelled during iteration
    private final java.util.List<Command> m_toCancel = new java.util.ArrayList<>();
    private boolean m_isIterating = false;

    // Loop boundary diagnostics
    private static final org.areslib.faults.AresAlert loopTimeAlert = new org.areslib.faults.AresAlert("Loop execution exceeding 10ms deadline!", org.areslib.faults.AresAlert.AlertType.WARNING);

    // Button bindings loops
    private final java.util.List<Runnable> m_buttons = new java.util.concurrent.CopyOnWriteArrayList<>();

    private CommandScheduler() {}

    /**
     * Registers a button binding loop to be executed periodically by the scheduler.
     *
     * @param button a Runnable representing the button's polling loop.
     */
    public void addButton(Runnable button) {
        m_buttons.add(button);
    }

    /**
     * Returns the Singleton instance of the scheduler.
     *
     * @return the instance
     */
    public static synchronized CommandScheduler getInstance() {
        if (instance == null) {
            instance = new CommandScheduler();
        }
        return instance;
    }

    /**
     * Registers subsystems with the scheduler. This must be called for the subsystem's periodic
     * block to run when the scheduler is run, and for the subsystem's default command to be
     * scheduled.
     *
     * @param subsystems the subsystem to register
     */
    public void registerSubsystem(Subsystem... subsystems) {
        m_subsystems.addAll(Arrays.asList(subsystems));
    }

    /**
     * Un-registers subsystems with the scheduler. The subsystem's periodic will no longer run,
     * and its default command will no longer be scheduled.
     *
     * @param subsystems the subsystem to un-register
     */
    public void unregisterSubsystem(Subsystem... subsystems) {
        m_subsystems.removeAll(Arrays.asList(subsystems));
    }

    /**
     * Returns an unmodifiable set of all registered subsystems.
     * Useful for external backend enumerators like physical simulators.
      * @return The current value.
     */
    public java.util.Set<Subsystem> getSubsystems() {
        return java.util.Collections.unmodifiableSet(m_subsystems);
    }

    /**
     * Sets the default command for a subsystem. The default command will run whenever there is no
     * other command currently scheduled that requires the subsystem.
     *
     * @param subsystem the subsystem whose default command should be set
     * @param defaultCommand the default command to associate with the subsystem
     */
    public void setDefaultCommand(Subsystem subsystem, Command defaultCommand) {
        if (!defaultCommand.getRequirements().contains(subsystem)) {
            defaultCommand.addRequirements(subsystem);
        }
        m_defaultCommands.put(subsystem, defaultCommand);
    }

    /**
     * Schedules a command for execution.
     *
     * @param command the command to schedule
     */
    public void schedule(Command command) {
        if (m_isIterating) {
            m_toSchedule.add(command);
            return;
        }

        if (m_scheduledCommands.contains(command)) {
            return; // Already scheduled
        }

        // Check requirements and interrupt conflicting commands
        Set<Subsystem> requirements = command.getRequirements();
        for (Subsystem requirement : requirements) {
            if (m_requirements.containsKey(requirement)) {
                Command conflicting = m_requirements.get(requirement);
                conflicting.end(true);
                m_scheduledCommands.remove(conflicting);
                for (Subsystem conflictingReq : conflicting.getRequirements()) {
                    m_requirements.remove(conflictingReq);
                }
            }
        }

        command.initialize();
        m_scheduledCommands.add(command);
        for (Subsystem requirement : requirements) {
            m_requirements.put(requirement, command);
        }
    }

    /**
     * Cancels a command. The scheduler will call its end() method and un-schedule it.
     *
     * @param command the command to cancel
     */
    public void cancel(Command command) {
        if (m_isIterating) {
            m_toCancel.add(command);
            return;
        }

        if (!m_scheduledCommands.contains(command)) {
            return;
        }

        command.end(true);
        m_scheduledCommands.remove(command);
        for (Subsystem requirement : command.getRequirements()) {
            m_requirements.remove(requirement);
        }
    }

    /**
     * Checks whether a specific command is currently scheduled and running.
     * <p>This is the safe way to test if a separately-scheduled command has completed,
     * as opposed to calling {@code command.isFinished()} directly, which can race
     * with the scheduler's own removal logic.
     *
     * @param command The command to query.
     * @return True if the command is currently in the scheduled commands set.
     */
    public boolean isScheduled(Command command) {
        return m_scheduledCommands.contains(command);
    }

    /**
     * Runs a single iteration of the scheduler. The execution occurs in the following order:
     *
     * <p>1. Subsystem periodic methods are called.
     * <p>2. Default commands are scheduled for subsystems that have no acting command.
     * <p>3. Button binding loops are polled.
     * <p>4. Scheduled commands' execute() methods are called.
     * <p>5. Commands that have finished their run are removed, and their end() methods are called.
     */
    public void run() {
        long loopStart = System.nanoTime();

        // 1. Run subsystem periodics
        for (Subsystem subsystem : m_subsystems) {
            subsystem.periodic();
        }

        // 2. Schedule default commands
        for (Subsystem subsystem : m_subsystems) {
            if (!m_requirements.containsKey(subsystem) && m_defaultCommands.containsKey(subsystem)) {
                schedule(m_defaultCommands.get(subsystem));
            }
        }

        // 3. Execute button loops
        for (Runnable button : m_buttons) {
            button.run();
        }

        // 4. Execute commands
        m_isIterating = true;
        Set<Command> commandsToRemove = new LinkedHashSet<>();
        for (Command command : m_scheduledCommands) {
            command.execute();
            if (command.isFinished()) {
                command.end(false);
                commandsToRemove.add(command);
            }
        }
        m_isIterating = false;

        // 5. Clean up finished commands
        for (Command command : commandsToRemove) {
            m_scheduledCommands.remove(command);
            for (Subsystem req : command.getRequirements()) {
                m_requirements.remove(req);
            }
        }

        // 6. Schedule buffered commands
        for (Command command : m_toSchedule) {
            schedule(command);
        }
        m_toSchedule.clear();

        // 7. Cancel buffered commands
        for (Command command : m_toCancel) {
            cancel(command);
        }
        m_toCancel.clear();

        // 8. Loop Timing Diagnostics
        double loopTimeMs = (System.nanoTime() - loopStart) / 1_000_000.0;
        org.areslib.telemetry.AresAutoLogger.recordOutput("Ares/LoopTime_ms", loopTimeMs);
        loopTimeAlert.set(loopTimeMs > 10.0);
    }

    /**
     * Cancels all currently-scheduled commands.
     */
    public void cancelAll() {
        for (Command command : m_scheduledCommands) {
            command.end(true);
        }
        m_scheduledCommands.clear();
        m_requirements.clear();
    }

    /**
     * Fully resets the scheduler, clearing ALL registered subsystems, default commands,
     * button bindings, and scheduled commands. This MUST be called between OpMode transitions
     * (e.g., Auto → TeleOp) to prevent duplicate subsystem registrations and stale bindings.
     */
    public void reset() {
        cancelAll();
        m_subsystems.clear();
        m_defaultCommands.clear();
        m_buttons.clear();
        m_toSchedule.clear();
        m_toCancel.clear();
    }
}

package org.areslib.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless JUnit 5 tests for the {@link CommandScheduler} execution engine.
 * Covers scheduling, cancellation, interruption, default commands, button bindings,
 * subsystem periodic execution, and full reset lifecycle.
 */
public class CommandSchedulerTest {

    private CommandScheduler scheduler;

    @BeforeEach
    void setup() {
        scheduler = CommandScheduler.getInstance();
        scheduler.reset();
    }

    // ========== Helper classes ==========

    /** A minimal concrete subsystem for testing. */
    static class TestSubsystem extends SubsystemBase {
        int periodicCount = 0;
        @Override
        public void periodic() {
            periodicCount++;
        }
    }

    /** A command that counts execute() calls and finishes after N ticks. */
    static class CountingCommand extends Command {
        final int ticksToFinish;
        int initCount = 0;
        int executeCount = 0;
        int endCount = 0;
        boolean wasInterrupted = false;

        CountingCommand(int ticksToFinish, Subsystem... requirements) {
            this.ticksToFinish = ticksToFinish;
            addRequirements(requirements);
        }

        @Override public void initialize() { initCount++; }
        @Override public void execute() { executeCount++; }
        @Override public void end(boolean interrupted) {
            endCount++;
            wasInterrupted = interrupted;
        }
        @Override public boolean isFinished() { return executeCount >= ticksToFinish; }
    }

    // ========== Scheduling Tests ==========

    @Test
    void testScheduleCallsInitialize() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand cmd = new CountingCommand(5, sub);
        scheduler.schedule(cmd);

        assertEquals(1, cmd.initCount, "initialize() should be called exactly once on schedule");
        assertTrue(scheduler.isScheduled(cmd));
    }

    @Test
    void testScheduleRunsExecute() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand cmd = new CountingCommand(3, sub);
        scheduler.schedule(cmd);

        scheduler.run(); // Tick 1: execute
        assertEquals(1, cmd.executeCount);
        assertTrue(scheduler.isScheduled(cmd));

        scheduler.run(); // Tick 2: execute
        assertEquals(2, cmd.executeCount);
        assertTrue(scheduler.isScheduled(cmd));

        scheduler.run(); // Tick 3: execute → isFinished → end(false)
        assertEquals(3, cmd.executeCount);
        assertEquals(1, cmd.endCount);
        assertFalse(cmd.wasInterrupted, "Command should end normally, not interrupted");
        assertFalse(scheduler.isScheduled(cmd));
    }

    @Test
    void testDuplicateScheduleIgnored() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand cmd = new CountingCommand(10, sub);
        scheduler.schedule(cmd);
        scheduler.schedule(cmd); // Should be ignored

        assertEquals(1, cmd.initCount, "initialize() should not be called twice");
    }

    // ========== Cancellation Tests ==========

    @Test
    void testCancelCallsEndWithInterrupted() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand cmd = new CountingCommand(100, sub);
        scheduler.schedule(cmd);
        assertTrue(scheduler.isScheduled(cmd));

        scheduler.cancel(cmd);
        assertEquals(1, cmd.endCount);
        assertTrue(cmd.wasInterrupted, "Cancelled command should receive interrupted=true");
        assertFalse(scheduler.isScheduled(cmd));
    }

    @Test
    void testCancelNonScheduledCommandIsNoOp() {
        CountingCommand cmd = new CountingCommand(5);
        // Should not throw
        scheduler.cancel(cmd);
        assertEquals(0, cmd.endCount);
    }

    @Test
    void testCancelAll() {
        TestSubsystem sub1 = new TestSubsystem();
        TestSubsystem sub2 = new TestSubsystem();
        scheduler.registerSubsystem(sub1, sub2);

        CountingCommand cmd1 = new CountingCommand(100, sub1);
        CountingCommand cmd2 = new CountingCommand(100, sub2);
        scheduler.schedule(cmd1);
        scheduler.schedule(cmd2);

        scheduler.cancelAll();
        assertFalse(scheduler.isScheduled(cmd1));
        assertFalse(scheduler.isScheduled(cmd2));
        assertEquals(1, cmd1.endCount);
        assertEquals(1, cmd2.endCount);
    }

    // ========== Interruption (Requirement Conflict) Tests ==========

    @Test
    void testNewCommandInterruptsExistingOnSameSubsystem() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand cmd1 = new CountingCommand(100, sub);
        CountingCommand cmd2 = new CountingCommand(100, sub);

        scheduler.schedule(cmd1);
        assertTrue(scheduler.isScheduled(cmd1));

        // Scheduling cmd2 on the same subsystem should interrupt cmd1
        scheduler.schedule(cmd2);
        assertFalse(scheduler.isScheduled(cmd1), "Old command should be removed");
        assertTrue(cmd1.wasInterrupted, "Old command should be interrupted");
        assertTrue(scheduler.isScheduled(cmd2), "New command should be scheduled");
    }

    // ========== Default Command Tests ==========

    @Test
    void testDefaultCommandScheduledWhenIdle() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand defaultCmd = new CountingCommand(Integer.MAX_VALUE, sub);
        scheduler.setDefaultCommand(sub, defaultCmd);

        scheduler.run(); // Should schedule + execute the default command
        assertTrue(scheduler.isScheduled(defaultCmd));
        assertEquals(1, defaultCmd.executeCount);
    }

    @Test
    void testDefaultCommandNotScheduledWhenBusy() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand activeCmd = new CountingCommand(100, sub);
        CountingCommand defaultCmd = new CountingCommand(Integer.MAX_VALUE, sub);
        scheduler.setDefaultCommand(sub, defaultCmd);

        scheduler.schedule(activeCmd);
        scheduler.run();

        assertTrue(scheduler.isScheduled(activeCmd));
        assertFalse(scheduler.isScheduled(defaultCmd),
            "Default command should NOT run while subsystem is occupied");
    }

    @Test
    void testDefaultCommandResumesAfterActiveFinishes() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand activeCmd = new CountingCommand(1, sub); // Finishes after 1 tick
        CountingCommand defaultCmd = new CountingCommand(Integer.MAX_VALUE, sub);
        scheduler.setDefaultCommand(sub, defaultCmd);

        scheduler.schedule(activeCmd);
        scheduler.run(); // activeCmd executes → finishes → removed

        assertFalse(scheduler.isScheduled(activeCmd));

        scheduler.run(); // Now default should be scheduled
        assertTrue(scheduler.isScheduled(defaultCmd));
    }

    // ========== Subsystem Periodic Tests ==========

    @Test
    void testSubsystemPeriodicRunsEveryTick() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        scheduler.run();
        scheduler.run();
        scheduler.run();

        assertEquals(3, sub.periodicCount,
            "Subsystem periodic() should be called once per scheduler.run()");
    }

    @Test
    void testUnregisteredSubsystemPeriodicDoesNotRun() {
        TestSubsystem sub = new TestSubsystem();
        // NOT registered
        scheduler.run();
        scheduler.run();
        assertEquals(0, sub.periodicCount);
    }

    // ========== Button Binding Tests ==========

    @Test
    void testButtonLoopPolledEveryTick() {
        AtomicInteger pollCount = new AtomicInteger(0);
        scheduler.addButton(pollCount::incrementAndGet);

        scheduler.run();
        scheduler.run();
        scheduler.run();

        assertEquals(3, pollCount.get(), "Button loops should be polled once per tick");
    }

    // ========== Reset Tests ==========

    @Test
    void testResetClearsEverything() {
        TestSubsystem sub = new TestSubsystem();
        scheduler.registerSubsystem(sub);

        CountingCommand cmd = new CountingCommand(100, sub);
        scheduler.schedule(cmd);
        scheduler.addButton(() -> {});

        scheduler.reset();

        assertFalse(scheduler.isScheduled(cmd));
        assertTrue(scheduler.getSubsystems().isEmpty(),
            "All subsystems should be cleared after reset");

        // After reset, running should be a no-op (no subsystems, no buttons, no commands)
        scheduler.run();
        assertEquals(0, sub.periodicCount - 0); // periodic should not be called after unregister
    }

    // ========== InstantCommand Integration ==========

    @Test
    void testInstantCommandFinishesImmediately() {
        AtomicBoolean ran = new AtomicBoolean(false);
        InstantCommand cmd = new InstantCommand(() -> ran.set(true));

        scheduler.schedule(cmd);
        assertTrue(ran.get(), "InstantCommand should run its action during initialize()");

        scheduler.run(); // Should finish and be removed
        assertFalse(scheduler.isScheduled(cmd));
    }
}

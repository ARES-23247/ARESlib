package org.areslib.command.button;

import static org.junit.jupiter.api.Assertions.*;

import org.areslib.command.Command;
import org.areslib.command.CommandScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TriggerTest {

  private static class MockCommand extends Command {
    public boolean isScheduled = false;
    private boolean finished = false;

    @Override
    public void initialize() {
      isScheduled = true;
    }

    @Override
    public void end(boolean interrupted) {
      isScheduled = false;
    }

    @Override
    public boolean isFinished() {
      return finished;
    }
  }

  private boolean conditionState = false;

  @BeforeEach
  void setUp() {
    CommandScheduler.getInstance().reset();
    conditionState = false;
  }

  @AfterEach
  void tearDown() {
    CommandScheduler.getInstance().reset();
  }

  @Test
  void testOnTrue() {
    Trigger trigger = new Trigger(() -> conditionState);
    MockCommand command = new MockCommand();
    trigger.onTrue(command);

    // Initially false, nothing happens
    CommandScheduler.getInstance().run();
    assertFalse(command.isScheduled);

    // Edge false -> true
    conditionState = true;
    CommandScheduler.getInstance().run(); // Event fires and schedules
    CommandScheduler.getInstance().run(); // Initializer runs
    assertTrue(command.isScheduled);

    // Sustained true, should not re-fire
    CommandScheduler.getInstance().run();

    // Edge true -> false, command keeps running independently
    conditionState = false;
    CommandScheduler.getInstance().run();
    assertTrue(command.isScheduled);
  }

  @Test
  void testWhileTrue() {
    Trigger trigger = new Trigger(() -> conditionState);
    MockCommand command = new MockCommand();
    trigger.whileTrue(command);

    // False -> True edge
    conditionState = true;
    CommandScheduler.getInstance().run();
    CommandScheduler.getInstance().run(); // Init
    assertTrue(command.isScheduled);

    // True -> False edge cancels the command immediately
    conditionState = false;
    CommandScheduler.getInstance().run(); // Cancels the command
    assertFalse(command.isScheduled);
  }

  @Test
  void testOnFalse() {
    Trigger trigger = new Trigger(() -> conditionState);
    MockCommand command = new MockCommand();
    trigger.onFalse(command);

    conditionState = true;
    CommandScheduler.getInstance().run();
    assertFalse(command.isScheduled);

    // Edge true -> false
    conditionState = false;
    CommandScheduler.getInstance().run(); // Fire
    CommandScheduler.getInstance().run(); // Init
    assertTrue(command.isScheduled);
  }
}

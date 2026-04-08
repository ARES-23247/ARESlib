package org.areslib.command;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandGroupsTest {

  private static class MockCommand extends Command {
    public boolean initCalled = false;
    public int executeCount = 0;
    public boolean endCalled = false;
    public boolean isInterrupted = false;
    private boolean finished = false;

    @Override
    public void initialize() {
      initCalled = true;
    }

    @Override
    public void execute() {
      executeCount++;
    }

    @Override
    public void end(boolean interrupted) {
      endCalled = true;
      isInterrupted = interrupted;
    }

    @Override
    public boolean isFinished() {
      return finished;
    }

    public void finish() {
      this.finished = true;
    }
  }

  @BeforeEach
  void setUp() {
    CommandScheduler.getInstance().reset();
  }

  @AfterEach
  void tearDown() {
    CommandScheduler.getInstance().reset();
  }

  @Test
  void testSequentialCommandGroup() {
    MockCommand c1 = new MockCommand();
    MockCommand c2 = new MockCommand();
    SequentialCommandGroup seq = new SequentialCommandGroup(c1, c2);

    CommandScheduler.getInstance().schedule(seq);
    CommandScheduler.getInstance().run();

    assertTrue(c1.initCalled);
    assertEquals(1, c1.executeCount);
    assertFalse(c2.initCalled);

    // c1 finishes next loop
    c1.finish();
    CommandScheduler.getInstance().run();

    assertTrue(c1.endCalled);
    assertFalse(c1.isInterrupted);
    assertTrue(c2.initCalled);

    // c2 finishes next loop
    c2.finish();
    CommandScheduler.getInstance().run();
    assertTrue(c2.endCalled);
    // CommandScheduler cleans up finished commands by calling end(), which resets index to -1.
    // Therefore, we check if it was removed from the scheduler instead.
    assertFalse(CommandScheduler.getInstance().isScheduled(seq));
  }

  @Test
  void testParallelCommandGroup() {
    MockCommand c1 = new MockCommand();
    MockCommand c2 = new MockCommand();
    ParallelCommandGroup par = new ParallelCommandGroup(c1, c2);

    CommandScheduler.getInstance().schedule(par);
    CommandScheduler.getInstance().run();

    assertTrue(c1.initCalled);
    assertTrue(c2.initCalled);
    assertEquals(1, c1.executeCount);
    assertEquals(1, c2.executeCount);
    assertFalse(par.isFinished());

    c1.finish();
    CommandScheduler.getInstance().run();
    assertTrue(c1.endCalled);
    assertEquals(2, c2.executeCount); // c2 still running
    assertFalse(par.isFinished());

    c2.finish();
    CommandScheduler.getInstance().run();
    assertTrue(c2.endCalled);
    assertTrue(par.isFinished());
  }

  @Test
  void testParallelRaceGroup() {
    MockCommand c1 = new MockCommand();
    MockCommand c2 = new MockCommand();
    ParallelRaceGroup race = new ParallelRaceGroup(c1, c2);

    CommandScheduler.getInstance().schedule(race);
    CommandScheduler.getInstance().run();

    assertTrue(c1.initCalled);
    assertTrue(c2.initCalled);

    // Make c1 finish fast
    c1.finish();
    CommandScheduler.getInstance().run();

    assertTrue(c1.endCalled);
    // c2 should be interrupted because the race finished
    assertTrue(c2.endCalled);
    assertTrue(c2.isInterrupted);
    assertTrue(race.isFinished());
  }

  @Test
  void testParallelDeadlineGroup() {
    MockCommand deadline = new MockCommand();
    MockCommand other = new MockCommand();
    ParallelDeadlineGroup group = new ParallelDeadlineGroup(deadline, other);

    CommandScheduler.getInstance().schedule(group);
    CommandScheduler.getInstance().run();

    assertTrue(deadline.initCalled);
    assertTrue(other.initCalled);
    assertFalse(group.isFinished());

    deadline.finish();
    CommandScheduler.getInstance().run();

    assertTrue(deadline.endCalled);
    assertFalse(deadline.isInterrupted);
    assertTrue(other.endCalled);
    assertTrue(other.isInterrupted); // other gets cancelled when deadline finishes
    assertTrue(group.isFinished());
  }
}

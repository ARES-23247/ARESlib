package org.areslib.core.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.areslib.command.Command;
import org.areslib.command.CommandScheduler;
import org.areslib.command.Subsystem;
import org.areslib.core.AresRobot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Boilerplate template for end-to-end Simulation Integration Testing. Uses AresSimulator to
 * decouple physics simulation loops while the main JUnit thread iterates the CommandScheduler,
 * accurately mimicking true robot match environments.
 */
class SimulationIntegrationTest {

  @BeforeEach
  void setUp() {
    // Essential: Configure the robot system state to simulation mode
    AresRobot.setSimulation(true);
    // Clean any residual commands mapped from previous tests
    CommandScheduler.getInstance().reset();
  }

  @AfterEach
  void tearDown() {
    // Cleanly stop the background physics thread
    AresSimulator.stopPhysicsSim();
    CommandScheduler.getInstance().reset();
  }

  @Test
  @DisplayName("Verify CommandScheduler processes commands and subsystem periods via Simulator")
  void testStandardSchedulerRoutine() throws InterruptedException {
    // 1. Boot up the Physics Engine
    // This spins up a background thread that will continuously invoke
    // simulationPeriodic() on all registered subsystems at 200 Hz (5ms)
    AresSimulator.startPhysicsSim(5);

    // Given: A dummy subsystem representing a physical mechanism
    AtomicInteger mainPeriodicCount = new AtomicInteger();
    AtomicInteger simPeriodicCount = new AtomicInteger();

    Subsystem dummySubsystem =
        new Subsystem() {
          @Override
          public void periodic() {
            mainPeriodicCount.incrementAndGet();
          }

          @Override
          public void simulationPeriodic() {
            simPeriodicCount.incrementAndGet();
          }
        };

    CommandScheduler.getInstance().registerSubsystem(dummySubsystem);

    // Given: An autonomous routine sequence
    AtomicInteger executeCount = new AtomicInteger();
    Command mockAutoRoutine =
        new Command() {
          @Override
          public void initialize() {}

          @Override
          public void execute() {
            executeCount.incrementAndGet();
          }

          @Override
          public void end(boolean interrupted) {}

          @Override
          public boolean isFinished() {
            return executeCount.get() >= 50; // Finishes after 50 main loops (~1.0 sec)
          }
        };

    // 2. Schedule the exact autonomous routine used on the field
    CommandScheduler.getInstance().schedule(mockAutoRoutine);

    // 3. Spool the main virtual thread forward iteratively
    // Simulate 1.5 seconds of game time (75 ticks at 20ms = 1500ms)
    for (int i = 0; i < 75; i++) {
      CommandScheduler.getInstance().run();
      // Wait 20ms to behave EXACTLY like an FTC opMode loop
      Thread.sleep(20);
    }

    // 4. Assert the physical simulator state reached the expected destination
    // Main thread should execute exactly 75 times
    assertEquals(75, mainPeriodicCount.get(), "Main loop should iterate exactly 75 times");

    // The auto routine should have finished right at 50 loops
    assertEquals(50, executeCount.get(), "Auto command should terminate correctly after 50 loops");

    // The simulation thread should run decoupled and asynchronously in the background.
    // It runs at 5ms periods versus 20ms loop periods. 75 * 20ms = 1500ms.
    // At 5ms, 1500 / 5 = 300 expected sim ticks.
    // Note: Asserting threading ranges loosely because CPU scheduling can drift a few ms.
    assertTrue(
        simPeriodicCount.get() > 100,
        "Background physics simulator should have executed periodically");
  }
}

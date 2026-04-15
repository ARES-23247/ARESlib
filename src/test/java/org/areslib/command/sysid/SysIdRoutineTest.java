package org.areslib.command.sysid;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.areslib.core.AresRobot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SysIdRoutineTest {

  private AtomicReference<Double> appliedVoltage;
  private double simPosition;
  private double simVelocity;

  @BeforeEach
  public void setup() {
    appliedVoltage = new AtomicReference<>(0.0);
    simPosition = 1.0;
    simVelocity = 2.0;

    // Force 20ms fixed timestep
    AresRobot.setSimulation(true);
  }

  @AfterEach
  public void teardown() {
    AresRobot.setSimulation(false);
  }

  @Test
  public void testSysIdQuasistatic() {
    SysIdRoutine.Config config = new SysIdRoutine.Config();
    config.rampRateVoltsPerSec = 1.0;
    config.stepVoltageVolts = 5.0;
    config.timeoutSeconds = 5.0;

    Consumer<Double> voltageInput = appliedVoltage::set;
    Supplier<Double> positionOutput = () -> simPosition;
    Supplier<Double> velocityOutput = () -> simVelocity;

    SysIdRoutine.Mechanism mechanism =
        new SysIdRoutine.Mechanism(voltageInput, positionOutput, velocityOutput);
    SysIdRoutine routine = new SysIdRoutine(config, mechanism);

    org.areslib.command.Command quasistaticCmd =
        routine.quasistatic(SysIdRoutine.Direction.FORWARD);

    quasistaticCmd.initialize();

    try {
      // Step 1: Wait roughly 50ms
      Thread.sleep(50);
      quasistaticCmd.execute();

      // Since time is dynamic, just assert voltage is actively ramping
      assertTrue(appliedVoltage.get() > 0.0, "Voltage should be ramping up");
      assertFalse(quasistaticCmd.isFinished());

      // Advance past timeout (5.0 seconds timeout -> sleep ~5050ms)
      Thread.sleep(5050);
      quasistaticCmd.execute();
      assertTrue(quasistaticCmd.isFinished());
    } catch (InterruptedException e) {
      fail("Test interrupted");
    }

    quasistaticCmd.end(false);
    assertEquals(0.0, appliedVoltage.get(), 1e-6); // Must set hardware to 0
  }

  @Test
  public void testSysIdDynamic() {
    SysIdRoutine.Config config = new SysIdRoutine.Config();
    config.rampRateVoltsPerSec = 1.0;
    config.stepVoltageVolts = 7.0;
    config.timeoutSeconds = 2.0;

    Consumer<Double> voltageInput = appliedVoltage::set;
    Supplier<Double> positionOutput = () -> simPosition;
    Supplier<Double> velocityOutput = () -> simVelocity;

    SysIdRoutine.Mechanism mechanism =
        new SysIdRoutine.Mechanism(voltageInput, positionOutput, velocityOutput);
    SysIdRoutine routine = new SysIdRoutine(config, mechanism);

    // Run reverse dynamic
    org.areslib.command.Command dynamicCmd = routine.dynamic(SysIdRoutine.Direction.REVERSE);

    dynamicCmd.initialize();

    // Execute applies instant max step voltage in reverse
    dynamicCmd.execute();
    assertEquals(-7.0, appliedVoltage.get(), 1e-6);

    dynamicCmd.execute();
    assertEquals(-7.0, appliedVoltage.get(), 1e-6);

    dynamicCmd.end(false);
    assertEquals(0.0, appliedVoltage.get(), 1e-6);
  }
}

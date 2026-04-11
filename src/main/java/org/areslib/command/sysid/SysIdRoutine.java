package org.areslib.command.sysid;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.areslib.command.Subsystem;

/**
 * An orchestrator object for WPILib-style System Identification (SysId).
 *
 * <p>Using quasistatic (voltage ramp) and dynamic (voltage step) tests, SysId mathematically
 * derives perfect Feedforward constants (kS, kV, kA) to give you drift-free and immediate
 * mechanical reaction times.
 */
public class SysIdRoutine {

  /** The direction of the motor test. */
  public enum Direction {
    FORWARD(1.0),
    REVERSE(-1.0);
    public final double multiplier;

    Direction(double multiplier) {
      this.multiplier = multiplier;
    }
  }

  /** Configuration for the generated tests. */
  public static class Config {
    /** Rate at which the voltage ramps during the quasistatic test (e.g. 1.0 Volts/sec) */
    public double rampRateVoltsPerSec = 1.0;

    /** The instantaneous voltage applied during the dynamic step test (e.g. 7.0 Volts) */
    public double stepVoltageVolts = 7.0;

    /** Maximum duration of any test before aborting */
    public double timeoutSeconds = 10.0;

    public Config() {}
  }

  /** Binds the mechanism IO specifically for testing. */
  public static class Mechanism {
    public final Consumer<Double> voltageInput;
    public final Supplier<Double> positionOutput;
    public final Supplier<Double> velocityOutput;
    public final Subsystem[] requirements;

    /**
     * @param voltageInput The method to apply RAW VOLTAGE to the motor. MUST NOT be constrained by
     *     PID or Profile.
     * @param positionOutput Method supplying current mechanism position (meters or radians).
     * @param velocityOutput Method supplying current mechanism velocity (m/s or rad/s).
     * @param requirements The Subsystem(s) to require to prevent conflicts.
     */
    public Mechanism(
        Consumer<Double> voltageInput,
        Supplier<Double> positionOutput,
        Supplier<Double> velocityOutput,
        Subsystem... requirements) {
      this.voltageInput = voltageInput;
      this.positionOutput = positionOutput;
      this.velocityOutput = velocityOutput;
      this.requirements = requirements;
    }
  }

  private final Config config;
  private final Mechanism mechanism;

  /**
   * Creates a new System Identification routine factory for a specific mechanism.
   *
   * @param config The routine profile configuration (voltage ramp rates, step values, timeouts).
   * @param mechanism The hardware IO binding for the target mechanism.
   */
  public SysIdRoutine(Config config, Mechanism mechanism) {
    this.config = config;
    this.mechanism = mechanism;
  }

  /**
   * Creates a command to run a quasistatic (slow voltage ramp) test. Finds kS (static friction) and
   * kV (velocity constant).
   *
   * @param direction The direction value.
   * @return The current value.
   */
  public org.areslib.command.Command quasistatic(Direction direction) {
    return new SysIdCommand(config, mechanism, true, direction);
  }

  /**
   * Creates a command to run a dynamic (instant voltage step) test. Finds kA (acceleration root
   * constant).
   *
   * @param direction The direction value.
   * @return The current value.
   */
  public org.areslib.command.Command dynamic(Direction direction) {
    return new SysIdCommand(config, mechanism, false, direction);
  }
}

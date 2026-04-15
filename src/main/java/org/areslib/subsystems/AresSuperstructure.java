package org.areslib.subsystems;

import java.util.function.Supplier;
import org.areslib.command.Command;
import org.areslib.command.InstantCommand;
import org.areslib.command.SubsystemBase;
import org.areslib.core.StateMachine;
import org.areslib.math.geometry.Pose2d;
import org.areslib.telemetry.AresAutoLogger;

/**
 * Base class for a robot Superstructure.
 *
 * <p>A Superstructure is a "meta-subsystem" that coordinates multiple smaller subsystems (Intake,
 * Arm, Shooter) into high-level logical states. It uses an internal {@link StateMachine} to ensure
 * transition safety and deterministic behavior.
 *
 * @param <S> The enum type representing superstructure states.
 */
public abstract class AresSuperstructure<S extends Enum<S>> extends SubsystemBase {

  protected final StateMachine<S> stateMachine;
  protected final Supplier<Pose2d> poseSupplier;

  /**
   * Constructs the superstructure.
   *
   * @param name Name for telemetry keys.
   * @param enumClass The state enum class.
   * @param initialState The state to start in.
   * @param poseSupplier Supplier for robot field pose.
   */
  public AresSuperstructure(
      String name, Class<S> enumClass, S initialState, Supplier<Pose2d> poseSupplier) {

    this.stateMachine = new StateMachine<>(name, enumClass, initialState);
    this.poseSupplier = poseSupplier;
  }

  /**
   * Request a transition to a new state. Transition will be rejected if illegal in the state
   * machine transition table.
   *
   * @param target The desired state.
   * @return A command to perform the request.
   */
  public Command requestState(S target) {
    return new InstantCommand(() -> stateMachine.requestTransition(target), this);
  }

  /**
   * Force the superstructure into a state, bypassing transition table logic.
   *
   * @param target The state to force.
   */
  public void forceState(S target) {
    stateMachine.forceState(target);
  }

  @Override
  public void periodic() {
    // 1. Update State Machine
    stateMachine.update();

    // 2. User Logic
    S currentState = stateMachine.getState();
    updateMechanisms(currentState);

    // 3. Telemetry
    AresAutoLogger.recordOutput(
        "Superstructure/" + stateMachine.getName() + "/CurrentState", currentState.name());
  }

  /**
   * Subclasses implement this to drive subsystem targets based on the current superstructure state.
   *
   * @param state The current active state.
   */
  protected abstract void updateMechanisms(S state);

  public S getState() {
    return stateMachine.getState();
  }
}

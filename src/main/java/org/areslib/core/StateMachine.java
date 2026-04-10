package org.areslib.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * A lightweight, enum-based finite state machine for managing subsystem complexity.
 *
 * <p>Provides entry/exit/during actions per state, conditional and timeout transitions, validated
 * transition tables, and automatic telemetry logging. State changes and illegal transition attempts
 * are both logged for debugging.
 *
 * <p><b>Validated Transition Mode:</b> When transitions are explicitly registered via {@link
 * #addTransition}, {@link #addValidBidirectional}, {@link #addWildcardTo}, or {@link
 * #addWildcardFrom}, the machine operates in <em>validated mode</em>. In this mode, any transition
 * not explicitly whitelisted is rejected and logged as an error. If no transitions are explicitly
 * registered, all transitions are allowed (legacy behavior).
 *
 * <pre>{@code
 * enum IntakeState { IDLE, EXTENDING, GRIPPING, RETRACTING, STOWED }
 *
 * StateMachine<IntakeState> sm = new StateMachine<>("Intake", IntakeState.class, IntakeState.IDLE);
 * sm.addTransition(IDLE, EXTENDING);
 * sm.addTransition(EXTENDING, GRIPPING);
 * sm.addValidBidirectional(GRIPPING, RETRACTING);
 * sm.addWildcardTo(IDLE); // any state can abort to IDLE
 *
 * sm.onEntry(EXTENDING, () -> slides.setPower(1.0));
 * sm.during(GRIPPING, () -> gripper.holdPosition());
 * sm.transition(EXTENDING, GRIPPING, () -> slides.atTarget());
 * sm.transitionAfter(GRIPPING, RETRACTING, 0.5); // 500ms timeout
 *
 * // In periodic():
 * sm.update();
 * }</pre>
 *
 * @param <S> The enum type representing the states.
 */
public class StateMachine<S extends Enum<S>> {

  private final String m_name;
  private final Class<S> m_enumClass;
  private S m_currentState;
  private S m_previousState;
  private double m_stateEntryTime;
  private int m_totalTransitionCount;

  private final Map<S, List<Runnable>> m_entryActions = new HashMap<>();
  private final Map<S, List<Runnable>> m_exitActions = new HashMap<>();
  private final Map<S, List<Runnable>> m_duringActions = new HashMap<>();
  private final List<Transition<S>> m_transitions = new ArrayList<>();

  /** Validated transition table. Null means "allow all" (unvalidated mode). */
  private Map<S, EnumSet<S>> m_validTransitions = null;

  private BiConsumer<S, S> m_onTransitionCallback = (from, to) -> {};

  /**
   * Constructs a StateMachine with a name, enum class, and initial state.
   *
   * <p>The name is used in telemetry log keys for identifying which state machine produced a log
   * entry. The enum class is required for {@link EnumSet} construction in the validated transition
   * table.
   *
   * @param name Human-readable name for log output (e.g., "Intake", "Elevator").
   * @param enumClass The enum class token (e.g., {@code IntakeState.class}).
   * @param initialState The state the machine starts in.
   */
  public StateMachine(String name, Class<S> enumClass, S initialState) {
    m_name = name;
    m_enumClass = enumClass;
    m_currentState = initialState;
    m_previousState = initialState;
    m_stateEntryTime = now();
    m_totalTransitionCount = 0;
  }

  /**
   * Constructs a StateMachine with an auto-derived name and initial state.
   *
   * <p>The name is derived from the enum class simple name. This constructor is kept for backwards
   * compatibility.
   *
   * @param initialState The state the machine starts in.
   */
  public StateMachine(S initialState) {
    this(
        initialState.getDeclaringClass().getSimpleName(),
        (Class<S>) initialState.getDeclaringClass(),
        initialState);
  }

  // ── Validated Transition Table ──────────────────────────────────────────────

  private void ensureTransitionMap() {
    if (m_validTransitions == null) {
      m_validTransitions = new HashMap<>();
    }
  }

  /**
   * Registers a one-directional transition from {@code from} to {@code to}.
   *
   * <p>Once any transition is explicitly registered, the machine enters <em>validated mode</em>:
   * only whitelisted transitions are allowed; all others are rejected and logged.
   *
   * @param from The source state.
   * @param to The destination state.
   */
  public void addTransition(S from, S to) {
    ensureTransitionMap();
    m_validTransitions.computeIfAbsent(from, k -> EnumSet.noneOf(m_enumClass)).add(to);
  }

  /**
   * Registers transitions in both directions between {@code a} and {@code b}.
   *
   * @param a The first state.
   * @param b The second state.
   */
  public void addValidBidirectional(S a, S b) {
    addTransition(a, b);
    addTransition(b, a);
  }

  /**
   * Makes {@code targetState} reachable from every state in the enum.
   *
   * @param targetState The state that all others can transition to.
   */
  public void addWildcardTo(S targetState) {
    for (S state : m_enumClass.getEnumConstants()) {
      addTransition(state, targetState);
    }
  }

  /**
   * Makes {@code fromState} able to transition to every state in the enum.
   *
   * @param fromState The state that can go anywhere.
   */
  public void addWildcardFrom(S fromState) {
    for (S state : m_enumClass.getEnumConstants()) {
      addTransition(fromState, state);
    }
  }

  /**
   * Checks whether a transition from the current state to {@code nextState} would be accepted.
   *
   * @param nextState The proposed destination state.
   * @return {@code true} if the transition is legal (including self-transitions).
   */
  public boolean isTransitionLegal(S nextState) {
    if (m_currentState == nextState) return true;
    if (m_validTransitions == null) return true; // Unvalidated mode: all transitions allowed
    EnumSet<S> valid = m_validTransitions.get(m_currentState);
    return valid != null && valid.contains(nextState);
  }

  // ── Configuration ──────────────────────────────────────────────────────────

  /**
   * Registers an action to run once when entering a state.
   *
   * @param state The state.
   * @param action The action.
   */
  public void onEntry(S state, Runnable action) {
    m_entryActions.computeIfAbsent(state, k -> new ArrayList<>()).add(action);
  }

  /**
   * Registers an action to run once when exiting a state.
   *
   * @param state The state.
   * @param action The action.
   */
  public void onExit(S state, Runnable action) {
    m_exitActions.computeIfAbsent(state, k -> new ArrayList<>()).add(action);
  }

  /**
   * Registers an action to run every loop tick while in a state.
   *
   * @param state The state.
   * @param action The action.
   */
  public void during(S state, Runnable action) {
    m_duringActions.computeIfAbsent(state, k -> new ArrayList<>()).add(action);
  }

  /**
   * Registers a conditional transition from one state to another.
   *
   * @param from The source state.
   * @param to The target state.
   * @param condition The condition that triggers the transition.
   */
  public void transition(S from, S to, BooleanSupplier condition) {
    m_transitions.add(new Transition<>(from, to, condition));
  }

  /**
   * Registers a timeout-based transition that fires after spending a given duration in the source
   * state.
   *
   * @param from The source state.
   * @param to The target state.
   * @param timeoutSeconds The time in seconds before auto-transitioning.
   */
  public void transitionAfter(S from, S to, double timeoutSeconds) {
    m_transitions.add(new Transition<>(from, to, () -> getTimeInState() >= timeoutSeconds));
  }

  /**
   * Registers a callback that fires on every accepted transition, receiving the source and
   * destination states.
   *
   * @param callback A {@link BiConsumer} that receives (fromState, toState).
   */
  public void setOnTransition(BiConsumer<S, S> callback) {
    m_onTransitionCallback = callback;
  }

  // ── Runtime ────────────────────────────────────────────────────────────────

  /** Evaluates transitions and executes state actions. Must be called every loop iteration. */
  public void update() {
    // Evaluate transitions (first match wins)
    for (Transition<S> t : m_transitions) {
      if (t.from == m_currentState && t.condition.getAsBoolean()) {
        requestTransition(t.to);
        break;
      }
    }

    // Execute during actions for the current state
    List<Runnable> duringActions = m_duringActions.get(m_currentState);
    if (duringActions != null) {
      for (Runnable action : duringActions) {
        action.run();
      }
    }
  }

  /**
   * Requests a state change. If validated transitions are configured and the transition is illegal,
   * the request is rejected and an error is logged.
   *
   * @param newState The desired state.
   * @return {@code true} if the transition was accepted, {@code false} if rejected.
   */
  public boolean requestTransition(S newState) {
    if (newState == m_currentState) return true;

    if (!isTransitionLegal(newState)) {
      org.areslib.telemetry.AresAutoLogger.recordOutput(
          "StateMachine/" + m_name + "/IllegalTransition",
          m_currentState.name() + " -> " + newState.name());
      return false;
    }

    executeTransition(newState);
    return true;
  }

  /**
   * Forces a state change, bypassing the validated transition table. Use with caution — this should
   * only be called for emergency resets or initialization.
   *
   * @param newState The state to transition to.
   */
  public void forceState(S newState) {
    if (newState == m_currentState) return;
    executeTransition(newState);
  }

  private void executeTransition(S newState) {
    // Run exit actions for old state
    List<Runnable> exitActions = m_exitActions.get(m_currentState);
    if (exitActions != null) {
      for (Runnable action : exitActions) {
        action.run();
      }
    }

    // Fire the transition callback
    m_onTransitionCallback.accept(m_currentState, newState);

    m_previousState = m_currentState;
    m_currentState = newState;
    m_stateEntryTime = now();
    m_totalTransitionCount++;

    // Log the transition
    org.areslib.telemetry.AresAutoLogger.recordOutput(
        "StateMachine/" + m_name, m_currentState.name());

    // Run entry actions for new state
    List<Runnable> entryActions = m_entryActions.get(m_currentState);
    if (entryActions != null) {
      for (Runnable action : entryActions) {
        action.run();
      }
    }
  }

  // ── Queries ─────────────────────────────────────────────────────────────────

  /**
   * Returns the current state.
   *
   * @return The current state.
   */
  public S getState() {
    return m_currentState;
  }

  /**
   * Returns the previous state.
   *
   * @return The previous state.
   */
  public S getPreviousState() {
    return m_previousState;
  }

  /**
   * Returns true if the machine is in the given state.
   *
   * @param state The state to check.
   * @return True if in the given state.
   */
  public boolean isInState(S state) {
    return m_currentState == state;
  }

  /**
   * Returns the time (in seconds) spent in the current state.
   *
   * @return Time in current state.
   */
  public double getTimeInState() {
    return now() - m_stateEntryTime;
  }

  /**
   * Returns the human-readable name of this state machine.
   *
   * @return The machine name.
   */
  public String getName() {
    return m_name;
  }

  /**
   * Returns the total number of accepted (non-self) transitions since construction.
   *
   * @return Cumulative transition count.
   */
  public int getTotalTransitionCount() {
    return m_totalTransitionCount;
  }

  private static double now() {
    return System.nanoTime() / 1_000_000_000.0;
  }

  // ── Internal types ──────────────────────────────────────────────────────────

  private static class Transition<S> {
    final S from;
    final S to;
    final BooleanSupplier condition;

    Transition(S from, S to, BooleanSupplier condition) {
      this.from = from;
      this.to = to;
      this.condition = condition;
    }
  }
}

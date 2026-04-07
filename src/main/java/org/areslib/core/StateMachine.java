package org.areslib.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * A lightweight, enum-based finite state machine for managing subsystem complexity.
 * <p>
 * Provides entry/exit/during actions per state, conditional transitions, and automatic
 * timeout transitions. State changes are logged to telemetry for debugging.
 *
 * <pre>{@code
 * enum IntakeState { IDLE, EXTENDING, GRIPPING, RETRACTING, STOWED }
 *
 * StateMachine<IntakeState> sm = new StateMachine<>(IntakeState.IDLE);
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

    private S m_currentState;
    private S m_previousState;
    private double m_stateEntryTime;

    private final Map<S, List<Runnable>> m_entryActions = new HashMap<>();
    private final Map<S, List<Runnable>> m_exitActions = new HashMap<>();
    private final Map<S, List<Runnable>> m_duringActions = new HashMap<>();
    private final List<Transition<S>> m_transitions = new ArrayList<>();

    /**
     * Constructs a StateMachine with the given initial state.
     *
     * @param initialState The state the machine starts in.
     */
    public StateMachine(S initialState) {
        m_currentState = initialState;
        m_previousState = initialState;
        m_stateEntryTime = now();
    }

    // ── Configuration ──────────────────────────────────────────────────────────

    /**
     * Registers an action to run once when entering a state.
     * @param state The state.
     * @param action The action.
     */
    public void onEntry(S state, Runnable action) {
        m_entryActions.computeIfAbsent(state, k -> new ArrayList<>()).add(action);
    }

    /**
     * Registers an action to run once when exiting a state.
     * @param state The state.
     * @param action The action.
     */
    public void onExit(S state, Runnable action) {
        m_exitActions.computeIfAbsent(state, k -> new ArrayList<>()).add(action);
    }

    /**
     * Registers an action to run every loop tick while in a state.
     * @param state The state.
     * @param action The action.
     */
    public void during(S state, Runnable action) {
        m_duringActions.computeIfAbsent(state, k -> new ArrayList<>()).add(action);
    }

    /**
     * Registers a conditional transition from one state to another.
     *
     * @param from      The source state.
     * @param to        The target state.
     * @param condition The condition that triggers the transition.
     */
    public void transition(S from, S to, BooleanSupplier condition) {
        m_transitions.add(new Transition<>(from, to, condition));
    }

    /**
     * Registers a timeout-based transition that fires after spending a given
     * duration in the source state.
     *
     * @param from           The source state.
     * @param to             The target state.
     * @param timeoutSeconds The time in seconds before auto-transitioning.
     */
    public void transitionAfter(S from, S to, double timeoutSeconds) {
        m_transitions.add(new Transition<>(from, to,
            () -> getTimeInState() >= timeoutSeconds));
    }

    // ── Runtime ────────────────────────────────────────────────────────────────

    /**
     * Evaluates transitions and executes state actions. Must be called every loop iteration.
     */
    public void update() {
        // Evaluate transitions (first match wins)
        for (Transition<S> t : m_transitions) {
            if (t.from == m_currentState && t.condition.getAsBoolean()) {
                transitionTo(t.to);
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
     * Forces a state change, running exit and entry actions.
     *
     * @param newState The state to transition to.
     */
    public void forceState(S newState) {
        transitionTo(newState);
    }

    private void transitionTo(S newState) {
        if (newState == m_currentState) return;

        // Run exit actions for old state
        List<Runnable> exitActions = m_exitActions.get(m_currentState);
        if (exitActions != null) {
            for (Runnable action : exitActions) {
                action.run();
            }
        }

        m_previousState = m_currentState;
        m_currentState = newState;
        m_stateEntryTime = now();

        // Log the transition
        org.areslib.telemetry.AresAutoLogger.recordOutput(
            "StateMachine/" + m_currentState.getClass().getSimpleName(),
            m_currentState.name()
        );

        // Run entry actions for new state
        List<Runnable> entryActions = m_entryActions.get(m_currentState);
        if (entryActions != null) {
            for (Runnable action : entryActions) {
                action.run();
            }
        }
    }

    // ── Queries ─────────────────────────────────────────────────────────────────

    /** Returns the current state.
     * @return The current state.
     */
    public S getState() {
        return m_currentState;
    }

    /** Returns the previous state.
     * @return The previous state.
     */
    public S getPreviousState() {
        return m_previousState;
    }

    /** Returns true if the machine is in the given state.
     * @param state The state to check.
     * @return True if in the given state.
     */
    public boolean isInState(S state) {
        return m_currentState == state;
    }

    /** Returns the time (in seconds) spent in the current state.
     * @return Time in current state.
     */
    public double getTimeInState() {
        return now() - m_stateEntryTime;
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

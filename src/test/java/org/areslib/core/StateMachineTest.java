package org.areslib.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateMachineTest {

    enum TestState { A, B, C }

    @Test
    void startsInInitialState() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        assertEquals(TestState.A, sm.getState());
    }

    @Test
    void conditionalTransition() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        sm.transition(TestState.A, TestState.B, () -> true);
        sm.update();
        assertEquals(TestState.B, sm.getState());
    }

    @Test
    void transitionDoesNotFireWhenFalse() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        sm.transition(TestState.A, TestState.B, () -> false);
        sm.update();
        assertEquals(TestState.A, sm.getState());
    }

    @Test
    void entryActionRuns() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        boolean[] ran = {false};
        sm.onEntry(TestState.B, () -> ran[0] = true);
        sm.transition(TestState.A, TestState.B, () -> true);
        sm.update();
        assertTrue(ran[0]);
    }

    @Test
    void exitActionRuns() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        boolean[] ran = {false};
        sm.onExit(TestState.A, () -> ran[0] = true);
        sm.transition(TestState.A, TestState.B, () -> true);
        sm.update();
        assertTrue(ran[0]);
    }

    @Test
    void duringActionRuns() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        int[] count = {0};
        sm.during(TestState.A, () -> count[0]++);
        sm.update();
        sm.update();
        sm.update();
        assertEquals(3, count[0]);
    }

    @Test
    void duringActionDoesNotRunInOtherState() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        int[] count = {0};
        sm.during(TestState.B, () -> count[0]++);
        sm.update();
        assertEquals(0, count[0]);
    }

    @Test
    void previousStateTracked() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        sm.transition(TestState.A, TestState.B, () -> true);
        sm.update();
        assertEquals(TestState.A, sm.getPreviousState());
        assertEquals(TestState.B, sm.getState());
    }

    @Test
    void forceStateChanges() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        sm.forceState(TestState.C);
        assertEquals(TestState.C, sm.getState());
    }

    @Test
    void forceStateRunsEntryAndExit() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        boolean[] exited = {false};
        boolean[] entered = {false};
        sm.onExit(TestState.A, () -> exited[0] = true);
        sm.onEntry(TestState.B, () -> entered[0] = true);
        sm.forceState(TestState.B);
        assertTrue(exited[0]);
        assertTrue(entered[0]);
    }

    @Test
    void isInState() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        assertTrue(sm.isInState(TestState.A));
        assertFalse(sm.isInState(TestState.B));
    }

    @Test
    void firstMatchWins() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        sm.transition(TestState.A, TestState.B, () -> true);
        sm.transition(TestState.A, TestState.C, () -> true); // should NOT fire
        sm.update();
        assertEquals(TestState.B, sm.getState());
    }

    @Test
    void chainedTransitions() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        sm.transition(TestState.A, TestState.B, () -> true);
        sm.transition(TestState.B, TestState.C, () -> true);
        sm.update(); // A -> B
        sm.update(); // B -> C
        assertEquals(TestState.C, sm.getState());
    }

    @Test
    void selfTransitionIgnored() {
        StateMachine<TestState> sm = new StateMachine<>(TestState.A);
        int[] count = {0};
        sm.onEntry(TestState.A, () -> count[0]++);
        sm.forceState(TestState.A); // same state — should be no-op
        assertEquals(0, count[0]);
    }
}

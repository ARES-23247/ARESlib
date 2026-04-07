package org.areslib.math.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrapezoidProfileTest {

    private static final double kEpsilon = 1e-4;
    private static final double kDt = 0.02; // 50Hz

    @Test
    void reachesGoalFromRest() {
        TrapezoidProfile profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(1.0, 2.0),
            new TrapezoidProfile.State(5.0, 0.0),
            new TrapezoidProfile.State(0.0, 0.0)
        );

        TrapezoidProfile.State state = null;
        for (int i = 0; i < 1000; i++) {
            state = profile.calculate(kDt);
            if (profile.isFinished()) break;
        }

        assertTrue(profile.isFinished(), "Profile should reach goal");
        assertEquals(5.0, state.position, kEpsilon);
        assertEquals(0.0, state.velocity, kEpsilon);
    }

    @Test
    void respectsMaxVelocity() {
        TrapezoidProfile profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(2.0, 10.0), // high accel, limited velocity
            new TrapezoidProfile.State(100.0, 0.0),
            new TrapezoidProfile.State(0.0, 0.0)
        );

        double maxObservedVelocity = 0.0;
        for (int i = 0; i < 2000; i++) {
            TrapezoidProfile.State state = profile.calculate(kDt);
            maxObservedVelocity = Math.max(maxObservedVelocity, Math.abs(state.velocity));
            if (profile.isFinished()) break;
        }

        assertTrue(maxObservedVelocity <= 2.0 + kEpsilon,
            "Velocity should not exceed max: " + maxObservedVelocity);
    }

    @Test
    void respectsMaxAcceleration() {
        TrapezoidProfile profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(10.0, 1.0), // high max vel, limited accel
            new TrapezoidProfile.State(10.0, 0.0),
            new TrapezoidProfile.State(0.0, 0.0)
        );

        double prevVelocity = 0.0;
        for (int i = 0; i < 2000; i++) {
            TrapezoidProfile.State state = profile.calculate(kDt);
            // Skip the final step where the profile snaps to goal (discontinuous velocity change)
            if (!profile.isFinished()) {
                double acceleration = Math.abs(state.velocity - prevVelocity) / kDt;
                assertTrue(acceleration <= 1.0 + kEpsilon,
                    "Acceleration exceeded max at step " + i + ": " + acceleration);
            }
            prevVelocity = state.velocity;
            if (profile.isFinished()) break;
        }
    }

    @Test
    void reverseDirection() {
        TrapezoidProfile profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(1.0, 2.0),
            new TrapezoidProfile.State(-3.0, 0.0),
            new TrapezoidProfile.State(0.0, 0.0)
        );

        TrapezoidProfile.State state = null;
        for (int i = 0; i < 1000; i++) {
            state = profile.calculate(kDt);
            if (profile.isFinished()) break;
        }

        assertTrue(profile.isFinished());
        assertEquals(-3.0, state.position, kEpsilon);
    }

    @Test
    void alreadyAtGoal() {
        TrapezoidProfile profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(1.0, 1.0),
            new TrapezoidProfile.State(5.0, 0.0),
            new TrapezoidProfile.State(5.0, 0.0)
        );

        assertTrue(profile.isFinished());
        TrapezoidProfile.State state = profile.calculate(kDt);
        assertEquals(5.0, state.position, kEpsilon);
        assertEquals(0.0, state.velocity, kEpsilon);
    }

    @Test
    void shortDistance() {
        // Very short movement — should not overshoot
        TrapezoidProfile profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(10.0, 100.0),
            new TrapezoidProfile.State(0.01, 0.0),
            new TrapezoidProfile.State(0.0, 0.0)
        );

        for (int i = 0; i < 100; i++) {
            profile.calculate(kDt);
            if (profile.isFinished()) break;
        }

        assertTrue(profile.isFinished());
        assertTrue(profile.getState().position <= 0.01 + kEpsilon,
            "Should not overshoot");
    }

    @Test
    void zeroDtReturnsCurrentState() {
        TrapezoidProfile profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(1.0, 1.0),
            new TrapezoidProfile.State(5.0, 0.0),
            new TrapezoidProfile.State(0.0, 0.0)
        );

        TrapezoidProfile.State state = profile.calculate(0.0);
        assertEquals(0.0, state.position, kEpsilon);
        assertEquals(0.0, state.velocity, kEpsilon);
    }

    @Test
    void profileIsMonotonic() {
        // Position should always increase when moving forward
        TrapezoidProfile profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(2.0, 5.0),
            new TrapezoidProfile.State(10.0, 0.0),
            new TrapezoidProfile.State(0.0, 0.0)
        );

        double prevPosition = 0.0;
        for (int i = 0; i < 2000; i++) {
            TrapezoidProfile.State state = profile.calculate(kDt);
            assertTrue(state.position >= prevPosition - kEpsilon,
                "Position should be monotonically increasing");
            prevPosition = state.position;
            if (profile.isFinished()) break;
        }
    }
}

---
name: areslib-statemachine
description: Documents the ARESLib2 enum-based StateMachine framework for managing subsystem complexity. Use when implementing intake sequences, scoring mechanisms, or any multi-phase subsystem logic instead of raw switch/case blocks.
---

# ARESLib2 StateMachine Framework


You are an expert mechanism engineer for Team ARES. When implementing multi-phase subsystem logic using the ARESLib2 StateMachine framework, adhere strictly to the following guidelines.
The `org.areslib.core.StateMachine<S>` class is a lightweight, enum-based finite state machine. **Do NOT implement raw `switch/case` state logic in subsystems** — always use this framework instead. It provides entry/exit/during actions, conditional transitions, timeout transitions, and automatic telemetry logging.

## 1. Basic Pattern

```java
import org.areslib.core.StateMachine;

public class IntakeSubsystem extends SubsystemBase {
    
    enum IntakeState { IDLE, EXTENDING, GRIPPING, RETRACTING, STOWED }
    
    private final StateMachine<IntakeState> sm = new StateMachine<>(IntakeState.IDLE);
    
    public IntakeSubsystem(IntakeIO io) {
        // Entry actions — run ONCE when entering a state
        sm.onEntry(IntakeState.EXTENDING, () -> io.setSlidePower(1.0));
        sm.onEntry(IntakeState.GRIPPING,  () -> io.closeGripper());
        sm.onEntry(IntakeState.RETRACTING,() -> io.setSlidePower(-1.0));
        sm.onEntry(IntakeState.STOWED,    () -> io.setSlidePower(0.0));
        
        // Exit actions — run ONCE when leaving a state
        sm.onExit(IntakeState.EXTENDING, () -> io.setSlidePower(0.0));
        
        // During actions — run EVERY loop tick while in a state
        sm.during(IntakeState.GRIPPING, () -> io.holdGripperPosition());
        
        // Conditional transitions — evaluated every tick
        sm.transition(IntakeState.EXTENDING, IntakeState.GRIPPING, 
            () -> io.slidesAtTarget());

        sm.transition(IntakeState.RETRACTING, IntakeState.STOWED, 
            () -> io.slidesAtHome());
        
        // Timeout transitions — auto-transition after N seconds
        sm.transitionAfter(IntakeState.GRIPPING, IntakeState.RETRACTING, 0.5);
    }
    
    @Override
    public void periodic() {
        sm.update();  // MUST be called every loop — evaluates transitions + runs during actions
    }
    
    // External trigger (e.g., from a button binding)
    public void startIntake() {
        sm.forceState(IntakeState.EXTENDING);
    }
}
```

## 2. API Reference

| Method | Purpose |
|:-------|:--------|
| `new StateMachine<>(initialState)` | Constructor — starts in the given state |
| `sm.onEntry(state, action)` | Register action that runs ONCE on state entry |
| `sm.onExit(state, action)` | Register action that runs ONCE on state exit |
| `sm.during(state, action)` | Register action that runs EVERY tick while in state |
| `sm.transition(from, to, condition)` | Add conditional transition (first match wins) |
| `sm.transitionAfter(from, to, seconds)` | Add timeout-based transition |
| `sm.update()` | Evaluate transitions + run during actions (call every loop) |
| `sm.forceState(state)` | Force an immediate state change (runs exit/entry actions) |
| `sm.getState()` | Returns current state enum |
| `sm.getPreviousState()` | Returns the state before the last transition |
| `sm.isInState(state)` | Boolean check for current state |
| `sm.getTimeInState()` | Seconds elapsed since entering current state |

## 3. Telemetry Integration

State transitions are **automatically logged** to AdvantageScope via:
```
StateMachine/IntakeState → "GRIPPING"
```
No manual `AresAutoLogger.recordOutput()` call is needed for state tracking.

## 4. Integration with Commands

Use `forceState()` from Command bindings to trigger multi-phase mechanisms:
```java
// In configureBindings():
driverGamepad.a().onTrue(new InstantCommand(() -> intake.startIntake()));
driverGamepad.b().onTrue(new InstantCommand(() -> intake.sm.forceState(IntakeState.IDLE)));
```

## 5. Anti-Patterns

### Don't: Use raw switch/case for multi-state logic
```java
// BAD — no entry/exit actions, no automatic telemetry, error-prone
switch (state) {
    case EXTENDING: 
        if (slides.atTarget()) state = GRIPPING;
        break;
}

// GOOD — use the StateMachine framework
sm.transition(EXTENDING, GRIPPING, () -> slides.atTarget());
```

### Don't: Forget to call sm.update()
```java
// BAD — transitions never fire, during actions never run
public void periodic() {
    // Missing sm.update()!
}
```

## Testing

```java
@Test
void testStateMachineTransitions() {
    enum TestState { IDLE, ACTIVE, DONE }
    StateMachine<TestState> sm = new StateMachine<>(TestState.IDLE);
    
    sm.setState(TestState.ACTIVE);
    assertEquals(TestState.ACTIVE, sm.getState());
    
    sm.setState(TestState.DONE);
    assertEquals(TestState.DONE, sm.getState());
}
```

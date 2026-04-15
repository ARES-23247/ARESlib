---
name: areslib-superstructure
description: Documents the AresSuperstructure base class for global robot state coordination. Use when coordinating multiple subsystems (intake, arm, slider) into logical states.
---

# ARESLib Superstructure Framework

The `AresSuperstructure` base class is a "meta-subsystem" that orchestrates multiple child subsystems into high-level logical states. It provides a standardized way to handle complex robot behaviors and safety overrides.

## 1. Architecture

A Superstructure should be the source of truth for robot state. It typically coordinates:
- **Intake**: Pivot, rollers, sensors.
- **Arm/Lift**: Slider position, wrist angle.
- **Shooter**: Flywheel speed, hood angle.

## 2. Implementation Pattern

```java
import org.areslib.subsystems.AresSuperstructure;

public class RobotSuperstructure extends AresSuperstructure<RobotState> {

    public enum RobotState { STOWED, INTAKE, SCORE_LOW, SCORE_HIGH, UNJAM, BEACHED }

    private final IntakeSubsystem intake;
    private final ElevatorSubsystem elevator;

    public RobotSuperstructure(IntakeSubsystem intake, ElevatorSubsystem elevator, java.util.function.Supplier<org.areslib.math.geometry.Pose2d> poseSupplier) {
        super("Superstructure", RobotState.class, RobotState.STOWED, poseSupplier);

        this.intake = intake;
        this.elevator = elevator;

        // Validated Transitions
        stateMachine.addTransition(RobotState.STOWED, RobotState.INTAKE);
        stateMachine.addTransition(RobotState.INTAKE, RobotState.STOWED);
        stateMachine.addTransition(RobotState.STOWED, RobotState.SCORE_LOW);
        // ... etc
    }

    @Override
    protected void updateMechanisms(RobotState state) {
        switch (state) {
            case INTAKE:
                intake.extend();
                elevator.goToBottom();
                break;
            case SCORE_HIGH:
                intake.retract();
                elevator.goToHeight(1.2);
                break;
                break;
            case UNJAM:
                intake.reverse();
                elevator.stop();
                break;
            case STOWED:
            default:
                intake.retract();
                elevator.goToBottom();
                break;
        }
    }
}
```

## 3. Implementation Logic

The base class manages the state machine and prevents invalid transitions. It provides a `requestState` command that can be easily bound to controller buttons.

## 4. Commands & Controls

```java
// Button binding in RobotContainer
operatorGamepad.a().onTrue(superstructure.requestState(RobotState.INTAKE));
operatorGamepad.y().onTrue(superstructure.requestState(RobotState.SCORE_HIGH));
```

## 5. Telemetry

Outputs are automatically logged to:
- `Superstructure/<Name>/CurrentState` (String)

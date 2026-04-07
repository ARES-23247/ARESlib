---
name: areslib-commands
description: Helps construct custom ARESLib2 WPILib-style commands, subsystem bases, and integration hooks avoiding FTClib overlaps. Use when creating new commands, configuring button bindings, scheduling autonomous sequences, or wiring subsystem requirements.
---

# ARESLib Native Command Architecture


You are a command architecture engineer for Team ARES. When creating commands, binding buttons, or scheduling autonomous sequences in ARESLib2, adhere strictly to the following guidelines.
ARESLib explicitly completely bypasses the legacy `FTCLib` library, shipping with a completely custom `org.areslib.command` WPILib-ported architecture. Do NOT use `com.arcrobotics.ftclib`. 

## 1. Subsystem Configuration
When generating subsystems:
- Extend `org.areslib.command.SubsystemBase`.
- Use standard `public void periodic()` loops to execute AdvantageKit updates.
- Override `public void simulationPeriodic()` for simulation-only logic (physics sync, sensor simulation). This is called by `AresSimulator`'s high-frequency physics thread, or by `CommandScheduler` at the loop rate if `AresSimulator` is not active.
- Keep state logic heavily encapsulated inside `[Subsystem]IO` components to ensure identical mock environments during Desktop Simulation.

## 2. Command Types & Imports
Always import these from `org.areslib.command`:
- `Command`
- `SequentialCommandGroup`, `ParallelCommandGroup`, `ParallelRaceGroup`, `ParallelDeadlineGroup`
- `ConditionalCommand` — runs one of two commands based on a boolean supplier
- `SelectCommand` — runs one command from a map based on a key supplier
- `InstantCommand`, `RunCommand`, `WaitCommand`, `PrintCommand`

Example basic subsystem and default command pattern:
```java
// DriveSystem must implement `org.areslib.command.SubsystemBase`
driveSystem.setDefaultCommand(
    new RunCommand(() -> driveSystem.drive(0, 0, 0), driveSystem)
);
```

## 3. CommandScheduler — The Execution Engine

The `CommandScheduler` is a singleton that orchestrates ALL subsystem periodics, command execution, and button polling. Its `run()` method executes in this strict order:

1. Subsystem `periodic()` methods
2. Default commands scheduled for idle subsystems
3. Button binding loops polled
4. Scheduled commands' `execute()` called
5. Finished commands removed, `end()` called
6. Loop time logged to `Ares/LoopTime_ms`

### Critical Lifecycle Methods
```java
CommandScheduler scheduler = CommandScheduler.getInstance();

// Register subsystems (REQUIRED for periodic() to run)
scheduler.registerSubsystem(driveSubsystem, elevatorSubsystem);

// Schedule a command
scheduler.schedule(new MyAutoCommand(drive));

// Set default commands
scheduler.setDefaultCommand(drive, new RunCommand(() -> drive.stop(), drive));

// Check if a command is running
boolean isRunning = scheduler.isScheduled(myCommand);

// Cancel everything
scheduler.cancelAll();

// FULL reset between OpModes (Auto → TeleOp)
scheduler.reset();  // Clears subsystems, defaults, buttons, and scheduled commands
```

### In your main loop:
```java
@Override
public void loop() {
    CommandScheduler.getInstance().run();  // MUST be called every loop
    AresTelemetry.update();               // Flush telemetry
}
```

## 4. `AresGamepad` Button Bindings

The `AresGamepad` wrapper provides WPILib-style `Trigger` objects for reactive button bindings. Triggers are lazily cached — safe to call repeatedly in `configureBindings()`.

### Available Trigger Accessors
```java
AresGamepad driver = new AresGamepad(gamepad1);

driver.a()            // A / Cross button
driver.b()            // B / Circle button  
driver.x()            // X / Square button
driver.y()            // Y / Triangle button
driver.leftBumper()   // Left bumper
driver.rightBumper()  // Right bumper
driver.dpadUp()       // D-pad directions
driver.dpadDown()
driver.dpadLeft()
driver.dpadRight()
```

### Binding Commands to Buttons
```java
// Runs command once when button is first pressed
driver.a().onTrue(new InstantCommand(() -> intake.startIntake()));

// Runs command continuously while held, cancels on release
driver.rightBumper().whileTrue(new RunCommand(() -> shooter.spinUp(), shooter));

// Runs command once when button is released
driver.b().onFalse(new InstantCommand(() -> gripper.open()));
```

### Axis Accessors (pre-inverted)
```java
driver.getLeftX()           // Left stick X [-1, 1]
driver.getLeftY()           // Left stick Y [-1, 1] (UP = POSITIVE, already inverted)
driver.getRightX()          // Right stick X
driver.getRightY()          // Right stick Y (UP = POSITIVE, already inverted)
driver.getLeftTriggerAxis() // Left trigger [0, 1]
driver.getRightTriggerAxis()// Right trigger [0, 1]
```

## 5. `addRequirements()` — Subsystem Exclusivity

Commands MUST declare their subsystem requirements to prevent conflicts:
```java
public class ScoreCommand extends Command {
    public ScoreCommand(ElevatorSubsystem elevator, ArmSubsystem arm) {
        addRequirements(elevator, arm);  // Ensures exclusive access
    }
}
```

If two commands require the same subsystem, the newly scheduled command **interrupts** the existing one (calls `end(true)` on it).

## 6. Path Following Command Hooks
Do not write custom `while(!follower.isBusy())` loops in standard Command execution blocks.
ARESLib abstracts path following through PathPlanner-based sequential chains.

> **Cross-Reference**: See the `pathplanner` skill for full details on AutoBuilder configuration, dummy shim requirements, and FTC coordinate offsets.

To schedule an autonomous routine:
```java
// Schedule a PathPlanner auto by name (loads from deploy/pathplanner/autos/)
CommandScheduler.getInstance().schedule(new PathPlannerAuto("SquareAuto"));
```

To create inline path-following commands:
```java
// Build a path command inline using AutoBuilder
PathPlannerPath path = PathPlannerPath.fromPathFile("MyPath");
Command followCmd = AutoBuilder.followPath(path);
CommandScheduler.getInstance().schedule(followCmd);
```

## 7. Testing

```java
@Test
void testCommandSchedulerLifecycle() {
    CommandScheduler.getInstance().reset();
    SubsystemBase mockSub = new SubsystemBase() {};
    CommandScheduler.getInstance().registerSubsystem(mockSub);
    
    InstantCommand cmd = new InstantCommand(() -> {}, mockSub);
    CommandScheduler.getInstance().schedule(cmd);
    assertTrue(CommandScheduler.getInstance().isScheduled(cmd));
    
    CommandScheduler.getInstance().run();
    assertFalse(CommandScheduler.getInstance().isScheduled(cmd));
}
```

# Contributing to ARESLib2

Welcome to ARESLib2, FTC Team's championship-grade Command-Based robot framework! We are excited to have you writing code for the 2026 season.

This repository isn't just standard Java—it is a simulator-validated, data-logged framework. Writing code here requires understanding a few core design rules. This guide will walk you through them so your code merges cleanly.

---

## 1. The "Subsystem IO" Rule

We **never** talk to hardware directly inside a Subsystem. If you instantiate a `DcMotorEx` directly inside your Subsystem, you break the simulator!

Instead, we use **Dependency Injection**. Every mechanism must be split into three core files:

### The Interface: `[Mechanism]IO.java`
This defines *what data* the mechanism needs to log and what commands it can receive.
```java
public interface ElevatorIO {
    public static class Inputs {
        public double positionMeters = 0.0;
        public double currentAmps = 0.0;
    }
    public default void updateInputs(Inputs inputs) {}
    public default void setVoltage(double volts) {}
}
```

### The Real Hardware: `[Mechanism]IOReal.java`
This is the ONLY place a Motor Controller or Physical Sensor can exist.
```java
public class ElevatorIOReal implements ElevatorIO {
    private final DcMotorEx motor;
    
    public ElevatorIOReal(HardwareMap hwMap) {
        motor = hwMap.get(DcMotorEx.class, "elevator");
    }

    @Override
    public void updateInputs(Inputs inputs) {
        inputs.positionMeters = motor.getCurrentPosition() / TICKS_PER_METER;
        inputs.currentAmps = motor.getCurrent(CurrentUnit.AMPS);
    }
}
```

### The Physics Engine: `[Mechanism]IOSim.java`
This is how we test offline! Wrap it in a dyn4j physics body or math model.
```java
public class ElevatorIOSim implements ElevatorIO {
    private double position = 0.0;
    private double velocity = 0.0;

    @Override
    public void updateInputs(Inputs inputs) {
        position += velocity * 0.02;
        inputs.positionMeters = position;
    }
}
```

### Starter Templates
Check `src/main/java/org/areslib/templates/` for ready-to-copy IO templates:
- `FlywheelIO.java` + `FlywheelIOSim.java` — Shooters, rollers, spinners
- `LinearMechanismIO.java` + `LinearMechanismIOSim.java` — Elevators, slides, lifts
- `RotaryMechanismIO.java` + `RotaryMechanismIOSim.java` — Arms, wrists, pivots

---

## 2. AdvantageScope Logging

If it isn't logged, it doesn't exist. Our framework makes the codebase completely deterministic.

Almost all your sensor data is handled automatically by the `updateInputs` routine above. However, if you are computing logic (like a target state, an odometry pose, or a trajectory), you must log it explicitly:

```java
// Inside your Subsystem's periodic() loop:
AresAutoLogger.recordOutput("Elevator/TargetHeight", targetHeightMeters);
AresAutoLogger.recordOutput("Elevator/AtGoal", Math.abs(current - target) < 0.01);
```

---

## 3. Formatting and Linting (Spotless)

We strictly enforce Google Java Style guidelines. If you try to push code with messy indents or unused imports, **GitHub Actions will reject your Pull Request.**

To fix formatting automatically:
```bash
# Windows:
.\gradlew.bat spotlessApply

# Mac/Linux:
./gradlew spotlessApply
```

---

## 4. Submitting a Pull Request

1. **Create a branch** (e.g. `feature/auto-align` or `bugfix/elevator-jitter`).
2. Run the simulator (`.\gradlew.bat runSim`) to verify your logic offline.
3. Execute `.\gradlew.bat build` to ensure no compile errors exist.
4. Stage, commit, and push your branch.
5. Create a Pull Request (PR) against the `main` branch.
6. The CI pipeline will automatically run all JUnit tests and check formatting. If everything passes, request a review from a Robotics Lead.

---

## 5. Writing Tests (Mandatory Hygiene)

All subsystem tests use **physics-backed `IOSim` implementations** and the `dyn4j` engine—never Mockito for mechanism behavior. Every test's `@BeforeEach` **must** call `AresTestHarness.reset()` to prevent static state bleed:

```java
import org.areslib.testing.AresTestHarness;

@BeforeEach
public void setUp() {
    AresTestHarness.reset();
    // ... construct your subsystems here
}

@AfterEach
public void tearDown() {
    AresTestHarness.cleanup();
}
```

> [!WARNING]
> Omitting `AresFaultManager.clear()` will cause phantom critical-fault flags to leak between test methods, causing random assertion failures that only appear when the full suite runs.

---

## 6. Constants Package

All tunable parameters should be centralized. Never hard-code magic numbers in subsystem files — always define them in a constants class inside your `teamcode/` package.

---

## 7. Troubleshooting

### Build fails with "cannot find symbol"
1. Run `.\gradlew.bat spotlessApply` to fix any formatting issues.
2. Run `.\gradlew.bat compileJava` to see the full error trace.
3. Check that you haven't accidentally imported a WPILib class that doesn't exist in FTC.

### Simulation window doesn't open
1. Check you have JDK 17 installed: `java -version`
2. Run `.\gradlew.bat runSim --info` for verbose output.
3. Ensure no other process is blocking port 3300 (AdvantageScope's NT4 port).

---

Happy Coding! 🚀

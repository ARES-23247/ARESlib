---
name: areslib-testing
description: Guidelines for constructing Headless JUnit 5 tests inside the ARESLib2 framework without relying on FTC Android dependencies or RobotCore. Use when verifying subsystem logic, ensuring mock IO classes function mathematically, or testing physics collisions.
---

# ARESLib Native Unit Testing


You are a test engineer for Team ARES. When writing headless JUnit 5 tests for ARESLib2 subsystems, commands, or physics simulations, adhere strictly to the following guidelines.
Testing FTC code presents a major hurdle: the standard WPILib and FTC SDK libraries assume you have active `HardwareMap` contexts and Android `Activity` lifecycles running. 
Because `ARESLib2` fully abstracts hardware via **AdvantageKit IO interfaces**, we can write blazing-fast, 100% headless Java desktop tests that bypass Android entirely!

## 1. Zero SDK Dependencies 
NEVER import anything from `com.qualcomm.robotcore.hardware.*` (e.g., `HardwareMap`, `DcMotorEx`) or `org.firstinspires.ftc.robotcore.*` when writing JUnit assertions in the `src/test/java/` directory.

- The IDE desktop Java compiler will crash natively attempting to execute tests that try to spin up `RobotCore`.
- All tests must use `org.junit.jupiter.api.Test` annotations via standard Junit5 configuration.

## 2. Using Subsystem Mock Interfaces
If you are testing Subsystem logic (like PID loops or state machines):
1. Instantiate the Subsystem by passing it either a mocked `IO` interface (using Mockito `mock([Name]IO.class)`) or an `IOSim` wrapper.
2. Manually step the periodic loop in your test: `subsystem.periodic()`.
3. Assert that the Subsystem called the expected output commands against the Mock IO.

**Mockito dependency:** Mockito is already configured in `build.gradle` under `testImplementation`. The IDE may show red squiggles for `org.mockito.*` imports — these resolve correctly at compile time via Gradle.

## 3. Testing Full Command Lifecycle with `CommandScheduler`

To test commands end-to-end through the scheduler:

```java
import org.areslib.command.CommandScheduler;

@BeforeEach
public void setup() {
    CommandScheduler.getInstance().reset();  // ALWAYS reset between tests
}

@Test
public void testCommandCompletesAfterConditionMet() {
    MySubsystem sub = new MySubsystem(mockIO);
    CommandScheduler.getInstance().registerSubsystem(sub);
    
    MyCommand cmd = new MyCommand(sub);
    CommandScheduler.getInstance().schedule(cmd);
    
    // Step the scheduler tick-by-tick
    CommandScheduler.getInstance().run();  // Tick 1: initialize + first execute
    assertTrue(CommandScheduler.getInstance().isScheduled(cmd));
    
    // Simulate the condition that makes isFinished() return true
    when(mockIO.atTarget()).thenReturn(true);
    
    CommandScheduler.getInstance().run();  // Tick 2: execute → isFinished → end
    assertFalse(CommandScheduler.getInstance().isScheduled(cmd));
}
```

## 4. High-Fidelity Physics Testing (`dyn4j`)
If you are testing `IOSim` logic natively (e.g., Lidar, Odometry drift, DriveBase interactions), use the built-in `AresPhysicsWorld`!

```java
import org.dyn4j.world.World;
import org.dyn4j.dynamics.Body;
import org.areslib.core.simulation.AresPhysicsWorld;

public class MySensorTest {
    private World<Body> world;

    @BeforeEach
    public void setup() {
        // ALWAYS reset the singleton instance between tests
        AresPhysicsWorld physicsWorld = AresPhysicsWorld.getInstance();
        physicsWorld.reset(); 
        this.world = physicsWorld.getWorld();
    }

    @Test
    public void runSimulation() {
        // Build mock dyn4j shapes and assert contact!
        Body testWall = new Body();
        testWall.addFixture(Geometry.createRectangle(1.0, 1.0));
        world.addBody(testWall);
        
        // Assert logic...
    }
}
```

## 5. Injecting Mock State 
When instantiating Simulated components (like `ArrayLidarIOSim`), utilize pure Java Suppliers to pipe continuous state variables (like location, battery voltage) natively into the simulation without requiring a true odometry stack.

Example: `new ArrayLidarIOSim(() -> currentMockOdometryState, world)` Allows you to teleport the Lidar arbitrarily through the grid testing raycast functionality instantly.

## 6. Running Tests

```bash
# Run a specific test class
.\gradlew test --tests org.areslib.examples.auto.DynamicAvoidanceAutoTest

# Run all tests
.\gradlew test

# Quick compile check (skip tests)
.\gradlew build -x test
```

## 7. Anti-Patterns

### Don't: Import Android SDK classes in test files
```java
// BAD — will crash the desktop JVM
import com.qualcomm.robotcore.hardware.HardwareMap;

// GOOD — use Mockito or IOSim wrappers
SubsystemIO mockIO = mock(SubsystemIO.class);
```

### Don't: Forget to reset CommandScheduler between tests
```java
// BAD — state leaks between test methods
@Test void test1() { scheduler.schedule(cmd1); }
@Test void test2() { /* cmd1 is still scheduled! */ }

// GOOD — reset in @BeforeEach
@BeforeEach void setup() { CommandScheduler.getInstance().reset(); }
```

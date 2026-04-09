---
name: gradle-ftc-desktop
description: Defines the Gradle build configuration that extracts FTC SDK .aar dependencies into JARs for desktop simulation. Use when troubleshooting build errors, adding new FTC dependencies, or configuring the desktop classpath for simulation and testing.
---

# Java SE Gradle Architecture Protection 


You are an expert build engineer for Team ARES. When troubleshooting build errors, adding FTC dependencies, or configuring the desktop classpath for simulation, adhere strictly to the following guidelines.
ARESLib2 runs FTC code on a desktop JVM for simulation and testing. Because the FTC SDK packages dependencies as Android `.aar` archives, the `build.gradle` contains a critical extraction pipeline that converts `.aar` → `.jar` automatically.

## 1. The Extraction Rule

> **NEVER modify the `.aar` extraction blocks in `build.gradle`.** They are fragile and essential.

The pipeline works by:
1. Intercepting `.aar` files from the `ftcAars` configuration
2. Extracting the inner `classes.jar` from each archive
3. Injecting the JAR back into the runtime classpath

This lets VS Code, IntelliJ, and the desktop JVM resolve FTC classes without Android.

## 2. Adding New Dependencies

```gradle
dependencies {
    // Standard Maven JAR — use 'implementation' normally
    implementation 'org.dyn4j:dyn4j:5.0.2'
    
    // FTC .aar dependency — MUST use 'ftcAars' configuration
    ftcAars 'com.acmerobotics.dashboard:dashboard:0.5.1@aar'
    
    // Test dependencies
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testImplementation 'org.mockito:mockito-core:5.11.0'
}
```

### When to use `ftcAars` vs `implementation`
| Dependency Type | Configuration | Example |
|:---|:---|:---|
| Standard Maven JAR | `implementation` | `dyn4j`, `JUnit`, `Gson` |
| FTC SDK `.aar` | `ftcAars` | `RobotCore`, `FtcCommon`, `dashboard` |
| Local JAR file | `implementation files(...)` | PathPlanner, custom libs |

## 3. Build Commands

```bash
# Full build (compile + test)
.\gradlew build

# Compile only (skip tests)
.\gradlew build -x test

# Run tests only
.\gradlew test

# Run a specific test
.\gradlew test --tests org.areslib.hardware.VisionIOSimTest

# Clean build artifacts
.\gradlew clean
```

## 4. Testing the Build Pipeline

```java
@Test
void testFtcSdkClassesAvailable() {
    // If the .aar extraction works, these classes should be loadable
    assertDoesNotThrow(() -> {
        Class.forName("com.qualcomm.robotcore.hardware.DcMotorEx");
    }, "FTC SDK classes should be on the classpath after .aar extraction");
}
```

## 5. Common Pitfalls

### Don't: Use `implementation` for `.aar` files
```gradle
// BAD — desktop JVM can't read .aar format
implementation 'com.acmerobotics.dashboard:dashboard:0.5.1@aar'

// GOOD — uses the extraction pipeline
ftcAars 'com.acmerobotics.dashboard:dashboard:0.5.1@aar'
```

### Don't: Import Android-only classes in sim/test code
```java
// BAD — crashes desktop JVM
import android.content.Context;

// GOOD — use ARESLib2 wrappers
import org.areslib.hardware.wrappers.DcMotorExWrapper;
```

### Don't: Reorganize the build.gradle unpacking blocks
If you see "cannot find symbol" errors for FTC classes, the fix is almost never to restructure the Gradle file. Instead:
1. Check if the dependency is in `ftcAars` (not `implementation`)
2. Run `.\gradlew clean build` to force re-extraction
3. Verify the `.aar` file exists in Gradle's cache


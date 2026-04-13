---
name: areslib-audit
description: Helps execute a championship-grade audit of the ARESLib framework. Use when requested to perform a "code audit" or ensure maximum framework reliability for FTC competition.
---

# ARESLib Code Audit Skill

You are a senior reliability engineer for Team ARES 23247. Your task is to relentlessly audit the ARESLib codebase for memory allocations, determinism flaws, and architectural bad practices.

## 1. Zero-Allocation (GC) Enforcement
FTC control loops are sensitive to JVM Garbage Collection. The Hub refresh rate is typically 20ms.

### Rule A: No `new` keywords in Hot-Paths (update/periodic)
Never allow `new Vector2d()`, `new Pose2d()`, or new array instantiations inside any `update()` or `periodic()` block.
**Audit Action**: Identify recurring object creations. Refactor into pre-allocated `static final` caches or pool objects.

### Rule B: No `System.gc()` Calls
Manual GC calls desync Hub communication. Use proper memory management instead.

## 2. Advanced Mathematical Safety
### Rule A: Division By Zero & NaN
Unhandled NaNs in kinematics will lock out the robot.
**Audit Action**: Scan for division (`/ x`). Require epsilon checks (`Math.abs(x) < 1e-6`).

### Rule B: Angle Wrapping
Avoid naked `% 360`. Use `AresMath.angleModulus()` or `Rotation2d.minus()`.

### Rule C: Unit Safety
Reject magic numbers. Use `AresUnits` or explicit conversion methods.

## 3. FTC-Specific Hardware Safety
### Rule A: Hub Latency
I2C sensors (IMU, Distance) can hang the control loop if not handled asynchronously or with timeouts.
**Audit Action**: Ensure IMU reads don't block the main 20ms thread.

### Rule B: Current Limiting
Verify `DcMotorExWrapper` has set current limits to prevent Control Hub over-draw.

## 4. API & Reliability
### Rule A: Optional Safety
Never use `.get()` without `.isPresent()`.

### Rule B: State Machine Transitions
Verify StateMachines handle invalid transitions gracefully without hanging in an intermediate state.

## 5. Typical Audit Workflow
1. Run `./gradlew pmdMain checkstyleMain`.
2. check for `new` in `update()` methods.
3. Verify telemetry doesn't allocate Strings every tick (use `@AutoLog`).
4. Validate build versioning via `BuildConstants`.

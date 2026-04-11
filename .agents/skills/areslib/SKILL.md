---
name: areslib
description: Helps find the right ARESLib skill for any task. Use when starting any ARESLib work to identify which domain-specific skill to follow.
---

You are an expert FTC Software Engineer for Team ARES. Before writing any code, consult this routing table to find the correct domain skill for your task.

## 0. Skill Maintenance

**CRITICAL RULE:** Whenever you make architectural or significant code changes to the library (e.g., adding a new pattern, changing a physics method, refactoring how a subsystem is structured), you **MUST** identify and update the relevant `SKILL.md` files in `.agents/skills/` to reflect the new truth. Keeping skills in sync with the repository is mandatory.

## Skill Routing Table

| Domain | Skill | Use When |
|:---|:---|:---|
| **Core Standards** | `areslib-core-standards` | Formatting, Never Nester rules, specific unit/variable naming, mathematics formatting |
| **Architecture** | `areslib-architecture` | IO patterns, coordinate systems, scaffolding new subsystems |
| **Drivetrain** | `areslib-drivetrain` | Swerve/mecanum/differential kinematics, odometry, drive tuning |
| **Autonomous** | `areslib-autonomous` | Path following, ghost replay, shoot-on-the-move, dynamic avoidance |
| **PathPlanner** | `pathplanner` | Dummy shim layer, AutoBuilder config, path constraints, FTC coordinate offsets |
| **Simulation** | `areslib-simulation` | dyn4j physics, `AresPhysicsWorld`, field obstacles, game pieces, LiDAR |
| **Vision** | `areslib-vision` | AprilTag pipelines, `VisionIO`, pose injection, camera fusion |
| **Commands** | `areslib-commands` | WPILib-style commands, button bindings, scheduling, subsystem requirements |
| **Bindings** | `areslib-bindings` | AresGamepad bind hooks, controller HTML/MD doc generation, binding registration |
| **State Machines** | `areslib-statemachine` | Enum-based `StateMachine` for multi-phase subsystem logic |
| **Math & Control** | `areslib-math` | PID, feedforward, motion profiles, filters, geometry, pose estimation |
| **Hardware** | `areslib-hardware` | Motor/sensor wrappers, odometry pods, coprocessor interfaces, `IOReal` |
| **Faults** | `areslib-faults` | `AresAlert`, `AresFaultManager`, `AresDiagnostics`, pre-match checks |
| **Telemetry** | `areslib-telemetry` | `AresAutoLogger`, log backends, `@AutoLog` IO pattern |
| **Testing** | `areslib-testing` | Headless JUnit 5, physics-integrated tests, mock-free IO testing |
| **CI/CD** | `areslib-ci` | GitHub Actions build/test pipeline, Java 17, Gradle caching |
| **Elite Mining** | `areslib-elite-mining` | Identifying, evaluating, and porting mechanics/math from elite teams |
| **Build System** | `gradle-ftc-desktop` | `.aar` extraction, desktop classpath, dependency configuration |
| **Deployment** | `robot-dev` | ADB, Control Hub deploy, OpMode control, log retrieval |
| **AdvantageScope Layouts** | `advantagescope-layouts` | Layout JSON, tab configuration, MCP tools |
| **AdvantageScope HUD** | `advantagescope-hud-sim` | Desktop HUD, gamepad viz, point cloud rendering |
| **Skill Authoring** | `skill-authoring` | Creating/updating AI skills for new subsystems |

## Quick Decision Tree

```
Building a new subsystem?     → areslib-architecture → areslib-hardware
Writing autonomous paths?     → areslib-autonomous → pathplanner
Configuring PathPlanner?      → pathplanner
Adding physics simulation?    → areslib-simulation
Configuring vision?           → areslib-vision
Writing tests?                → areslib-testing
Debugging build failures?     → gradle-ftc-desktop
Deploying to robot?           → robot-dev
Setting up AdvantageScope?    → advantagescope-layouts
Creating a new AI skill?      → skill-authoring
```

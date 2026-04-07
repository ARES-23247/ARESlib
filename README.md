# ARESLib2 Quickstart Template
[![Build Status](https://github.com/thehomelessguy/ARESLib2/actions/workflows/build.yml/badge.svg)](https://github.com/thehomelessguy/ARESLib2/actions)
Welcome to the ARESLib2 Quickstart! This template provides a full Command-Based FTC robot framework equipped with a physics simulator (Dyn4j) and AdvantageKit telemetry data-logging out of the box.

## How to Use this Template

1. Click the **"Use this template"** button on GitHub to create a copy of this repository for your team.
2. Clone your new repository onto your local machine.
3. Open the repository in your IDE of choice (such as VS Code or Android Studio).
4. Wait for Gradle to fully sync.

## Elite Features (Included)

ARESLib2 is packed with Einstein-tested capabilities that abstract complex kinematics away from the programmer:
- **Virtual Simulation Parity**: Fully modeled Dyn4j rigit-body multi-robot contact physics natively logged to AdvantageScope.
- **Dynamic On-The-Fly Pathing**: Native Pedro Pathing wrappers with automated Bezier bounding-box obstacle avoidance.
- **Ghost Mode**: Serialized teleop JSON macros for auto-recording and flawless re-playback.
- **Shoot-on-the-Move**: Feedforward kinematic aim calculators (target leading calculation).
- **Advanced State Tracking**: 2D custom bounding-box State Machine loopers and GC-free 1D Kalman Filters for optimal mechanical isolation.
- **Automated SysId**: Generates standardized quasistatic and dynamic WPILog routines to extract perfect $k_S, k_V, k_A$ Feedforwards.
- **Persistent Fault Management**: `AresFaultManager` natively tracks and categorizes hardware alerts, broadcasting to AdvantageScope and automatically trigging haptic/LED feedback.
- **Odometry & Localizer Independence**: Built-in generic hardware abstractions for modules like the GoBilda Pinpoint.

## Project Structure

This project uses a standard FRC Command-Based structure, relying heavily on encapsulation using Hardware IO Abstraction.

*   `src/main/java/org/areslib/` – The core framework wrappers, physical kinematics, and telemetry trackers.
*   `src/main/java/org/firstinspires/ftc/teamcode/` – Your actual highly-customizable robot code.
    *   `RobotContainer.java` - The central hub where hardware implementations of subsystems are swapped dynamically via `AresRobot.isSimulation()`.
    *   `Constants.java` - Centralized team logic.

## FRC-Style Hardware Abstraction

ARESLib2 leverages FRC AdvantageKit's IO paradigm. `RobotContainer.java` automatically decides whether to instantiate subsystems using Real Hardware Wrappers (like `SwerveModuleIOReal`) or Sim Physics Layers (like `SwerveModuleIOSim`) based on the active simulation flag. This allows you to completely isolate logic code from hardware code!

## Running the Simulator Locally

ARESLib2 comes with a fully functioning 2D physics simulation environment that allows you to test your OpModes and autonomous routines without a physical robot.

To run the simulator from a terminal:

```bash
# Windows
.\gradlew.bat runSim

# Mac/Linux
./gradlew runSim
```

## Exploring Data with AdvantageScope

All robot interactions (in simulation and in real life) output WPILog or RLog telemetry compatible with AdvantageScope.
1. Download [AdvantageScope](https://github.com/Mechanical-Advantage/AdvantageScope).
2. For live data, open AdvantageScope, connect to your robot's IP (or `localhost:3300` for simulation), and begin rendering kinematics and logs.
3. For offline data, drag and drop the `.wpilog` files (found in the root folder after running a sim) into AdvantageScope.

## Building and Deploying to the Robot

To physically deploy your code to a REV Control Hub:

```bash
.\gradlew.bat installDebug
```
Make sure you are connected to the Control Hub's Wi-Fi network before deploying.

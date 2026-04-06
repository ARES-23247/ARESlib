# ARESLib2 Quickstart Template

Welcome to the ARESLib2 Quickstart! This template provides a full Command-Based FTC robot framework equipped with a physics simulator (Dyn4j) and AdvantageKit telemetry data-logging out of the box.

## How to Use this Template

1. Click the **"Use this template"** button on GitHub to create a copy of this repository for your team.
2. Clone your new repository onto your local machine.
3. Open the repository in your IDE of choice (such as VS Code or Android Studio).
4. Wait for Gradle to fully sync.

## Project Structure

This project uses a standard FtcRobotController-style structure, but heavily emphasizes clean encapsulation of logic away from the raw Android SDK context.

*   `src/main/java/org/areslib/` – The core framework wrappers, mathematical kinematics, and subsystem templates.
*   `src/main/java/org/areslib/examples/` – Demonstrations and example code representing what you would normally put in your `TeamCode` folder. 
    *   `auto/` - Example PedroPathing autonomous routines (e.g. `DynamicAvoidanceAuto.java`).
    *   `elevator/` - Example subsystem simulating an elevator.

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

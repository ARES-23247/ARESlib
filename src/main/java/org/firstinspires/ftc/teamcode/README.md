# Teamcode Structure Guide

Welcome to the ARESLib Advanced Command-Based Template!

If you are coming from traditional FTC programming, the structure of this project might look unfamiliar. Instead of writing all your logic procedurally inside a `LinearOpMode`'s `while (opModeIsActive())` loop, we are utilizing the **WPILib Command-based framework**—the industry standard in the FIRST Robotics Competition (FRC).

## The RobotContainer Pattern

In traditional FTC code, teams often copy-paste their hardware initialization (e.g., `hardwareMap.get(...)`) across every single TeleOp and Autonomous OpMode. This leads to massive duplicated files and hard-to-track bugs.

We fix this by using the **`RobotContainer` pattern**:
- **`RobotContainer.java`** is the "single source of truth" for your robot's hardware. It initializes your Subsystems exactly once, configures odometry tracking, and binds Gamepad events (`driver.a().whileTrue(...)`).
- Your `MainTeleOp.java` and `MainAuto.java` OpModes are strictly launchers. Their *only* job is to instantiate `RobotContainer` and let the Command Scheduler take over the heavy lifting.

## Directory Layout

* **`RobotContainer.java`** - The core hardware configuration and trigger binding hub.
* **`teleop/`** - Holds the launcher OpMode (`MainTeleOp.java`) to run the robot via gamepads.
* **`auto/`** - Holds your Autonomous OpModes. The templates rely on `RobotContainer.getAutonomousCommand()` to dispatch the pre-selected pathing state machine.
* **`subsystems/`** - Encapsulates logic for discrete modules (e.g., `ElevatorSubsystem`). They handle internal PID limits, hardware wrapper interactions, and natively broadcast their telemetry strings.
    * Note the **IO Abstraction Pattern** (`ElevatorIO`/`ElevatorSubsystem`). This advanced architecture allows you to dynamically substitute simulated hardware logic without touching your main robot code.
* **`commands/`** - Isolated files that perform an exact action, often bridging multiple subsystems natively. 
    * *Example:* `AlignToTagCommand` grabs location data from the `VisionSubsystem` and dynamically pipes corrective velocity vectors into the `DriveSubsystem`.

## Quick Start

1. **Changing Hardware**: To rename a motor or add new hardware, open `RobotContainer.java` and modify the hardware mappings in the constructor.
2. **Adding Controls**: To change what a button does, examine `configureButtonBindings()` inside `RobotContainer.java`. We utilize functional reactive loops like `.onTrue()` and `.whileTrue()`.
3. **Writing an Auto**: Write your paths inside a new `Command` state-machine (see `TeamAutoCommand.java`), and orchestrate it out of `RobotContainer.getAutonomousCommand()`.

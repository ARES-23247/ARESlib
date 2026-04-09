package org.firstinspires.ftc.teamcode;

import static org.firstinspires.ftc.teamcode.Constants.DriveConstants.*;
import static org.firstinspires.ftc.teamcode.Constants.ElevatorConstants.*;
import static org.firstinspires.ftc.teamcode.Constants.VisionConstants.*;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.areslib.command.Command;
import org.areslib.command.CommandScheduler;
import org.areslib.core.AresRobot;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.coprocessors.OctoMode;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.hardware.wrappers.AresGamepad;
import org.areslib.hardware.wrappers.AresOctoQuadSensor;
import org.areslib.hardware.wrappers.CRServoWrapper;
import org.areslib.hardware.wrappers.DcMotorExWrapper;
import org.areslib.hardware.wrappers.LimelightVisionWrapper;
import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.kinematics.ChassisSpeeds;
import org.areslib.pathplanner.auto.AutoBuilder;
import org.areslib.pathplanner.util.HolonomicPathFollowerConfig;
import org.areslib.pathplanner.util.PIDConstants;
import org.areslib.pathplanner.util.ReplanningConfig;
import org.areslib.subsystems.controllers.examples.ControllerModeExample;
import org.areslib.subsystems.controllers.examples.ControllerModeManagerExample;
import org.areslib.subsystems.drive.AresDrivetrain;
import org.areslib.subsystems.drive.SwerveDriveSubsystem;
import org.areslib.subsystems.drive.SwerveModuleIOReal;
import org.areslib.subsystems.drive.SwerveModuleIOSim;
import org.areslib.subsystems.vision.AresVisionSubsystem;
import org.firstinspires.ftc.teamcode.commands.AlignToTagCommand;
import org.firstinspires.ftc.teamcode.commands.ElevatorToPositionCommand;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorIOReal;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorIOSim;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorSubsystem;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the OpModes
 * (MainTeleOp, MainAuto). Instead, the structure of the robot (including subsystems, commands, and
 * button mappings) should be declared here.
 */
public class RobotContainer {

  // Subsystems
  private final SwerveDriveSubsystem drive;
  private final ElevatorSubsystem elevator;
  private final AresVisionSubsystem vision;

  // Controller Mode Management
  private ControllerModeManagerExample controllerModes;
  // Hardware Integrations
  public final OdometryIO.OdometryInputs pinpointInputs = new OdometryIO.OdometryInputs();
  public final org.areslib.hardware.interfaces.VisionIO.VisionInputs visionInputs =
      new org.areslib.hardware.interfaces.VisionIO.VisionInputs();

  // Input Devices
  private final AresGamepad driver;
  private final AresGamepad operator;

  /**
   * The container for the robot. Contains subsystems, OI devices, and commands.
   *
   * @param hardwareMap The hardwareMap.
   * @param gamepad1 The driver gamepad.
   * @param gamepad2 The operator gamepad.
   */
  public RobotContainer(HardwareMap hardwareMap, Gamepad gamepad1, Gamepad gamepad2) {
    // Bulk caching initialization - vital for extreme performance loops
    if (!AresRobot.isSimulation()) {
      AresHardwareManager.initHardware(hardwareMap);
    }

    // 1. Instantiate Subsystems
    if (AresRobot.isSimulation()) {
      drive =
          new SwerveDriveSubsystem(
              SWERVE_CONFIG,
              new SwerveModuleIOSim(),
              new SwerveModuleIOSim(),
              new SwerveModuleIOSim(),
              new SwerveModuleIOSim());
      elevator = new ElevatorSubsystem(new ElevatorIOSim());
    } else {
      drive =
          new SwerveDriveSubsystem(
              SWERVE_CONFIG,
              new SwerveModuleIOReal(
                  new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "flDrive")),
                  new CRServoWrapper(hardwareMap.get(CRServo.class, "flTurn")),
                  new AresOctoQuadSensor(0, OctoMode.ENCODER),
                  new AresOctoQuadSensor(4, OctoMode.ABSOLUTE),
                  SWERVE_CONFIG.getDriveMetersPerTick()),
              new SwerveModuleIOReal(
                  new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "frDrive")),
                  new CRServoWrapper(hardwareMap.get(CRServo.class, "frTurn")),
                  new AresOctoQuadSensor(1, OctoMode.ENCODER),
                  new AresOctoQuadSensor(5, OctoMode.ABSOLUTE),
                  SWERVE_CONFIG.getDriveMetersPerTick()),
              new SwerveModuleIOReal(
                  new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "blDrive")),
                  new CRServoWrapper(hardwareMap.get(CRServo.class, "blTurn")),
                  new AresOctoQuadSensor(2, OctoMode.ENCODER),
                  new AresOctoQuadSensor(6, OctoMode.ABSOLUTE),
                  SWERVE_CONFIG.getDriveMetersPerTick()),
              new SwerveModuleIOReal(
                  new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "brDrive")),
                  new CRServoWrapper(hardwareMap.get(CRServo.class, "brTurn")),
                  new AresOctoQuadSensor(3, OctoMode.ENCODER),
                  new AresOctoQuadSensor(7, OctoMode.ABSOLUTE),
                  SWERVE_CONFIG.getDriveMetersPerTick()));

      elevator =
          new ElevatorSubsystem(
              new ElevatorIOReal(
                  new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "elevatorMotor")),
                  ELEVATOR_CONFIG.getMetersPerTick() // Configuration scalar for distance
                  ));
    }

    CommandScheduler.getInstance().registerSubsystem(drive);
    CommandScheduler.getInstance().registerSubsystem(elevator);

    if (!AresRobot.isSimulation()) {
      vision =
          new AresVisionSubsystem(
              new LimelightVisionWrapper(hardwareMap, "limelightFront", "limelightRear"),
              MIN_TARGET_AREA_PERCENT,
              MAX_TRUST_AREA_PERCENT);
    } else {
      vision =
          new AresVisionSubsystem(
              new org.areslib.hardware.wrappers.ArrayVisionIOSim(() -> getOdometryInputs()),
              MIN_TARGET_AREA_PERCENT,
              MAX_TRUST_AREA_PERCENT);
    }
    CommandScheduler.getInstance().registerSubsystem(vision);

    // Odometry Tracking Loop Integration
    // Removed pinpoint and follower tracking until PathPlanner integration

    // Initialize controller mode system (after gamepads are available)
    controllerModes = new ControllerModeManagerExample();
    controllerModes.configure();

    // 2. Map Gamepads (OpModes pass real gamepads or null context)
    if (gamepad1 != null && gamepad2 != null) {
      driver = new AresGamepad(gamepad1);
      operator = new AresGamepad(gamepad2);
      configureButtonBindings();
      configureDefaultCommands();
    } else {
      driver = null;
      operator = null;
    }

    initPathPlanner();
  }

  private void initPathPlanner() {
    HolonomicPathFollowerConfig config =
        new HolonomicPathFollowerConfig(
            new PIDConstants(5.0, 0.0, 0.0), // Translation PID
            new PIDConstants(5.0, 0.0, 0.0), // Rotation PID
            5.0, // Max module speed
            0.5, // Drive base radius
            new ReplanningConfig());

    // Center Origin FTC (0,0) mapped to PathPlanner Corner (1.8288, 1.8288)
    double ftcOffset = 1.8288;

    AutoBuilder.configureHolonomic(
        () ->
            new Pose2d(
                pinpointInputs.xMeters + ftcOffset,
                pinpointInputs.yMeters + ftcOffset,
                new Rotation2d(pinpointInputs.headingRadians)),
        (Pose2d setPose) -> {
          pinpointInputs.xMeters = setPose.getX() - ftcOffset;
          pinpointInputs.yMeters = setPose.getY() - ftcOffset;
          pinpointInputs.headingRadians = setPose.getRotation().getRadians();
        },
        () ->
            new ChassisSpeeds(
                drive.getCommandedVx(), drive.getCommandedVy(), drive.getCommandedOmega()),
        (ChassisSpeeds output) -> {
          drive.drive(
              output.vxMetersPerSecond, output.vyMetersPerSecond, output.omegaRadiansPerSecond);
        },
        config,
        () -> false, // Should Flip Path
        drive);
  }

  /** Use this method to define your button->command mappings. */
  private void configureButtonBindings() {
    // Controller mode switching (Team 254-inspired architecture)
    // Driver X button → SPEAKER mode
    driver
        .x()
        .onTrue(
            new Command() {
              @Override
              public void initialize() {
                controllerModes.setMode(ControllerModeExample.SPEAKER);
              }

              @Override
              public boolean isFinished() {
                return true;
              }
            });

    // Driver Y button → HP (Human Player) mode
    driver
        .y()
        .onTrue(
            new Command() {
              @Override
              public void initialize() {
                controllerModes.setMode(ControllerModeExample.HP);
              }

              @Override
              public boolean isFinished() {
                return true;
              }
            });

    // Operator X button → POOP (ground intake) mode
    operator
        .x()
        .onTrue(
            new Command() {
              @Override
              public void initialize() {
                controllerModes.setMode(ControllerModeExample.POOP);
              }

              @Override
              public boolean isFinished() {
                return true;
              }
            });

    // Operator Y button → CLIMB mode
    operator
        .y()
        .onTrue(
            new Command() {
              @Override
              public void initialize() {
                controllerModes.setMode(ControllerModeExample.CLIMB);
              }

              @Override
              public boolean isFinished() {
                return true;
              }
            });

    // Driver Align to Tag explicitly overrides manual driving while Held
    driver.a().whileTrue(new AlignToTagCommand(drive, vision, ALIGN_TARGET_AREA_PERCENT));

    // Reset field-centric yaw
    driver
        .y()
        .onTrue(
            new Command() {
              @Override
              public void initialize() {
                // Reset pose placeholder
              }

              @Override
              public boolean isFinished() {
                return true;
              }
            });

    // Operator Elevator Dispatch (onTrue ensures single fire execution)
    operator.dpadUp().onTrue(new ElevatorToPositionCommand(elevator, HIGH_POSITION_METERS));
    operator.dpadDown().onTrue(new ElevatorToPositionCommand(elevator, LOW_POSITION_METERS));

    // DECODE Simulator Test: Align and Shoot on the move
    // Targets the Red Classifier goal from DecodeFieldSim (1.5, 1.5) and calculates lead
    driver
        .rightBumper()
        .whileTrue(
            new org.areslib.command.autoaim.ShootOnTheMoveCommand(
                () ->
                    new org.areslib.math.geometry.Translation2d(
                        pinpointInputs.xMeters, pinpointInputs.yMeters),
                () ->
                    new ChassisSpeeds(
                        drive.getCommandedVx(), drive.getCommandedVy(), drive.getCommandedOmega()),
                new org.areslib.math.geometry.Translation2d(1.5, 1.5),
                12.0, // 12 m/s simulated projectile velocity
                (requiredHeading) -> {
                  // Simple P-Controller to snap the drivetrain to the target-leading heading
                  double error = requiredHeading.getRadians() - pinpointInputs.headingRadians;

                  // Wrap error to [-PI, PI] to prevent spins
                  while (error > Math.PI) error -= 2.0 * Math.PI;
                  while (error < -Math.PI) error += 2.0 * Math.PI;

                  double correctionOmega = error * 5.0; // P-gain

                  drive.driveFieldCentric(
                      driver.getLeftY() * MAX_FWD_SPEED,
                      driver.getLeftX() * MAX_STR_SPEED,
                      correctionOmega,
                      new Rotation2d(pinpointInputs.headingRadians));
                },
                drive));
  }

  /**
   * Map default subsystem states here. These commands will gracefully yield out-of-the-way when
   * standard triggered commands are fired.
   */
  private void configureDefaultCommands() {
    CommandScheduler.getInstance()
        .setDefaultCommand(
            drive,
            new Command() {
              public Command init() {
                addRequirements(drive);
                return this;
              }

              @Override
              public void execute() {
                drive.driveFieldCentric(
                    driver.getLeftY() * MAX_FWD_SPEED,
                    driver.getLeftX() * MAX_STR_SPEED,
                    driver.getRightX() * MAX_ROT_SPEED,
                    new org.areslib.math.geometry.Rotation2d(0) // Placeholder
                    );
              }
            }.init());
  }

  public AresDrivetrain getDrivetrain() {
    return drive;
  }

  public AresVisionSubsystem getVision() {
    return vision;
  }

  /**
   * Exposes the simulated Vision inputs for external manipulation.
   *
   * @return The vision inputs.
   */
  public org.areslib.hardware.interfaces.VisionIO.VisionInputs getVisionInputs() {
    return visionInputs;
  }

  /**
   * Exposes the simulated/real Odometry inputs for external manipulation.
   *
   * @return The odometry inputs.
   */
  public OdometryIO.OdometryInputs getOdometryInputs() {
    return pinpointInputs;
  }

  /**
   * Returns the controller mode manager for mode-specific logic integration.
   *
   * @return The controller mode manager.
   */
  public ControllerModeManagerExample getControllerModes() {
    return controllerModes;
  }
}

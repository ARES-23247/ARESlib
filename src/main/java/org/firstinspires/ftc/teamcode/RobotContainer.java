package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.areslib.command.CommandScheduler;
import org.areslib.command.Command;
import org.areslib.command.Subsystem;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.wrappers.AresGamepad;
import org.areslib.hardware.wrappers.DcMotorExWrapper;
import org.areslib.hardware.wrappers.CRServoWrapper;
import org.areslib.hardware.wrappers.LimelightVisionWrapper;
import org.areslib.hardware.wrappers.AresOctoQuadSensor;
import org.areslib.hardware.coprocessors.OctoMode;

import org.areslib.subsystems.drive.SwerveDriveSubsystem;
import org.areslib.subsystems.drive.SwerveModuleIOReal;
import org.areslib.subsystems.drive.SwerveModuleIOSim;
import org.areslib.subsystems.drive.AresDrivetrain;
import org.areslib.core.AresRobot;

import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorIOReal;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorIOSim;
import org.areslib.subsystems.vision.AresVisionSubsystem;
import org.areslib.subsystems.vision.AresSensorFusionSubsystem;

import static org.firstinspires.ftc.teamcode.Constants.ElevatorConstants.*;
import static org.firstinspires.ftc.teamcode.Constants.DriveConstants.*;
import static org.firstinspires.ftc.teamcode.Constants.VisionConstants.*;

import org.areslib.core.localization.AresFollower;
import org.areslib.hardware.wrappers.PinpointOdometryWrapper;
import org.areslib.hardware.interfaces.OdometryIO;

import org.firstinspires.ftc.teamcode.commands.TeamAutoCommand;
import org.firstinspires.ftc.teamcode.commands.ElevatorToPositionCommand;
import org.firstinspires.ftc.teamcode.commands.AlignToTagCommand;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the OpModes
 * (MainTeleOp, MainAuto). Instead, the structure of the robot (including subsystems, commands, 
 * and button mappings) should be declared here.
 */
public class RobotContainer {

    // Subsystems
    private final SwerveDriveSubsystem drive;
    private final ElevatorSubsystem elevator;
    private final AresVisionSubsystem vision;
    private final AresFollower follower;
    
    // Hardware Integrations
    private final PinpointOdometryWrapper pinpoint;
    public final OdometryIO.OdometryInputs pinpointInputs = new OdometryIO.OdometryInputs();
    public final org.areslib.hardware.interfaces.VisionIO.VisionInputs visionInputs = new org.areslib.hardware.interfaces.VisionIO.VisionInputs();

    // Input Devices
    private final AresGamepad driver;
    private final AresGamepad operator;

    /**
     * The container for the robot. Contains subsystems, OI devices, and commands.
     */
    public RobotContainer(HardwareMap hardwareMap, Gamepad gamepad1, Gamepad gamepad2) {
        // Bulk caching initialization - vital for extreme performance loops
        if (!AresRobot.isSimulation()) {
            AresHardwareManager.initHardware(hardwareMap);
        }

        // 1. Instantiate Subsystems
        if (AresRobot.isSimulation()) {
            drive = new SwerveDriveSubsystem(
                SWERVE_CONFIG,
                new SwerveModuleIOSim(),
                new SwerveModuleIOSim(),
                new SwerveModuleIOSim(),
                new SwerveModuleIOSim()
            );
            elevator = new ElevatorSubsystem(new ElevatorIOSim());
        } else {
            drive = new SwerveDriveSubsystem(
                SWERVE_CONFIG,
                new SwerveModuleIOReal(
                    new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "flDrive")), 
                    new CRServoWrapper(hardwareMap.get(CRServo.class, "flTurn")),      
                    new AresOctoQuadSensor(0, OctoMode.ENCODER),                       
                    new AresOctoQuadSensor(4, OctoMode.ABSOLUTE),
                    SWERVE_CONFIG.getDriveMetersPerTick()
                ),
                new SwerveModuleIOReal(
                    new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "frDrive")), 
                    new CRServoWrapper(hardwareMap.get(CRServo.class, "frTurn")),      
                    new AresOctoQuadSensor(1, OctoMode.ENCODER),                       
                    new AresOctoQuadSensor(5, OctoMode.ABSOLUTE),
                    SWERVE_CONFIG.getDriveMetersPerTick()                       
                ),
                new SwerveModuleIOReal(
                    new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "blDrive")), 
                    new CRServoWrapper(hardwareMap.get(CRServo.class, "blTurn")),      
                    new AresOctoQuadSensor(2, OctoMode.ENCODER),                       
                    new AresOctoQuadSensor(6, OctoMode.ABSOLUTE),
                    SWERVE_CONFIG.getDriveMetersPerTick()                       
                ),
                new SwerveModuleIOReal(
                    new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "brDrive")), 
                    new CRServoWrapper(hardwareMap.get(CRServo.class, "brTurn")),      
                    new AresOctoQuadSensor(3, OctoMode.ENCODER),                       
                    new AresOctoQuadSensor(7, OctoMode.ABSOLUTE),
                    SWERVE_CONFIG.getDriveMetersPerTick()                       
                )
            );
            
            elevator = new ElevatorSubsystem(
                new ElevatorIOReal(
                    new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "elevatorMotor")),
                    ELEVATOR_CONFIG.getMetersPerTick() // Configuration scalar for distance
                )
            );
        }
        
        CommandScheduler.getInstance().registerSubsystem(drive);
        CommandScheduler.getInstance().registerSubsystem(elevator);
        
        if (!AresRobot.isSimulation()) {
            vision = new AresVisionSubsystem(
                new LimelightVisionWrapper(hardwareMap, "limelight"),
                MIN_TARGET_AREA_PERCENT,
                MAX_TRUST_AREA_PERCENT
            );
        } else {
            vision = new AresVisionSubsystem(
                new org.areslib.hardware.wrappers.ArrayVisionIOSim(() -> getOdometryInputs()),
                MIN_TARGET_AREA_PERCENT,
                MAX_TRUST_AREA_PERCENT
            );
        }
        CommandScheduler.getInstance().registerSubsystem(vision);

        // Odometry Tracking Loop Integration
        if (!AresRobot.isSimulation()) {
            pinpoint = new PinpointOdometryWrapper(hardwareMap, "pinpoint");
            CommandScheduler.getInstance().registerSubsystem(new Subsystem() {
                @Override
                public void periodic() {
                    pinpoint.updateInputs(pinpointInputs);
                }
            });
        } else {
            pinpoint = null;
        }

        follower = new AresFollower(drive, pinpointInputs);
        CommandScheduler.getInstance().registerSubsystem(follower);

        // Register the background asynchronous sensor fusion algorithms
        CommandScheduler.getInstance().registerSubsystem(
            new AresSensorFusionSubsystem(follower, vision, MAX_VISION_TRUST_FACTOR)
        );

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
    }

    /**
     * Use this method to define your button->command mappings.
     */
    private void configureButtonBindings() {
        // Driver Align to Tag explicitly overrides manual driving while Held
        driver.a().whileTrue(new AlignToTagCommand(drive, vision, ALIGN_TARGET_AREA_PERCENT));
        
        // Reset field-centric yaw
        driver.y().onTrue(new Command() {
            @Override
            public void initialize() {
                follower.setPose(new com.pedropathing.geometry.Pose(follower.getPose().getX(), follower.getPose().getY(), 0));
            }
            @Override
            public boolean isFinished() { return true; }
        });
        
        // Operator Elevator Dispatch (onTrue ensures single fire execution)
        operator.dpadUp().onTrue(new ElevatorToPositionCommand(elevator, HIGH_POSITION_METERS));
        operator.dpadDown().onTrue(new ElevatorToPositionCommand(elevator, LOW_POSITION_METERS));
    }

    /**
     * Map default subsystem states here. These commands will gracefully yield 
     * out-of-the-way when standard triggered commands are fired.
     */
    private void configureDefaultCommands() {
        CommandScheduler.getInstance().setDefaultCommand(drive, new Command() {
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
                    new org.areslib.math.geometry.Rotation2d(follower.getPose().getHeading())
                );
            }
        }.init());
    }

    public AresDrivetrain getDrivetrain() {
        return drive;
    }

    /**
     * Re-binds buttons. Useful for switching drivers/operator controls mid-match.
     */
    public Command getRedLeftAutoCommand() {
        return new TeamAutoCommand(follower, elevator);
    }
    
    /**
     * Dispatcher for the Blue Right Starting Position
     */
    public Command getBlueRightAutoCommand() {
        // Here you would substitute `TeamAutoCommand` with your Blue Right specific trajectory chain!
        return new TeamAutoCommand(follower, elevator);
    }
    
    /**
     * Expose odometry interfaces to the OpMode launcher for manual resets.
     */
    public AresFollower getFollower() {
        return follower;
    }

    public AresVisionSubsystem getVision() {
        return vision;
    }

    /**
     * Exposes the simulated/real Odometry inputs for external manipulation.
     */
    public OdometryIO.OdometryInputs getOdometryInputs() {
        return pinpointInputs;
    }

    /**
     * Exposes the simulated Vision inputs for external manipulation.
     */
    public org.areslib.hardware.interfaces.VisionIO.VisionInputs getVisionInputs() {
        return visionInputs;
    }
}

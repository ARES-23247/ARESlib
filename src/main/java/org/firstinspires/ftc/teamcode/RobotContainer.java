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

import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorIOReal;
import org.firstinspires.ftc.teamcode.subsystems.vision.VisionSubsystem;

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
    private final VisionSubsystem vision;
    private final AresFollower follower;
    
    // Hardware Integrations
    private final PinpointOdometryWrapper pinpoint;
    private final OdometryIO.OdometryInputs pinpointInputs = new OdometryIO.OdometryInputs();

    // Input Devices
    private final AresGamepad driver;
    private final AresGamepad operator;

    /**
     * The container for the robot. Contains subsystems, OI devices, and commands.
     */
    public RobotContainer(HardwareMap hardwareMap, Gamepad gamepad1, Gamepad gamepad2) {
        // Bulk caching initialization - vital for extreme performance loops
        AresHardwareManager.initHardware(hardwareMap);

        // 1. Instantiate Subsystems
        drive = new SwerveDriveSubsystem(
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "flDrive")), 
                new CRServoWrapper(hardwareMap.get(CRServo.class, "flTurn")),      
                new AresOctoQuadSensor(0, OctoMode.ENCODER),                       
                new AresOctoQuadSensor(4, OctoMode.ABSOLUTE)                       
            ),
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "frDrive")), 
                new CRServoWrapper(hardwareMap.get(CRServo.class, "frTurn")),      
                new AresOctoQuadSensor(1, OctoMode.ENCODER),                       
                new AresOctoQuadSensor(5, OctoMode.ABSOLUTE)                       
            ),
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "blDrive")), 
                new CRServoWrapper(hardwareMap.get(CRServo.class, "blTurn")),      
                new AresOctoQuadSensor(2, OctoMode.ENCODER),                       
                new AresOctoQuadSensor(6, OctoMode.ABSOLUTE)                       
            ),
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "brDrive")), 
                new CRServoWrapper(hardwareMap.get(CRServo.class, "brTurn")),      
                new AresOctoQuadSensor(3, OctoMode.ENCODER),                       
                new AresOctoQuadSensor(7, OctoMode.ABSOLUTE)                       
            )
        );
        CommandScheduler.getInstance().registerSubsystem(drive);
        
        elevator = new ElevatorSubsystem(
            new ElevatorIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "elevatorMotor")),
                0.005 // Configuration scalar for distance
            )
        );
        CommandScheduler.getInstance().registerSubsystem(elevator);
        
        vision = new VisionSubsystem(
            new LimelightVisionWrapper(hardwareMap, "limelight")
        );
        CommandScheduler.getInstance().registerSubsystem(vision);

        // Odometry Tracking Loop Integration
        pinpoint = new PinpointOdometryWrapper(hardwareMap, "pinpoint");
        CommandScheduler.getInstance().registerSubsystem(new Subsystem() {
            @Override
            public void periodic() {
                pinpoint.updateInputs(pinpointInputs);
            }
        });

        follower = new AresFollower(drive, pinpointInputs);
        CommandScheduler.getInstance().registerSubsystem(follower);

        // Register the background asynchronous sensor fusion algorithms
        CommandScheduler.getInstance().registerSubsystem(
            new org.firstinspires.ftc.teamcode.subsystems.SensorFusionSubsystem(follower, vision)
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
        driver.a().whileTrue(new AlignToTagCommand(drive, vision, 5.0));
        
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
        operator.dpadUp().onTrue(new ElevatorToPositionCommand(elevator, 0.8));
        operator.dpadDown().onTrue(new ElevatorToPositionCommand(elevator, 0.0));
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
                    driver.getLeftY() * 3.0,
                    driver.getLeftX() * 3.0,
                    driver.getRightX() * 2.5,
                    new org.areslib.math.geometry.Rotation2d(follower.getPose().getHeading())
                );
            }
        }.init());
    }

    /**
     * Dispatcher for the Red Left Starting Position
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

    /**
     * Expose vision interface for pre-match target locking.
     */
    public VisionSubsystem getVision() {
        return vision;
    }
}

package org.areslib.teamcode.swerve;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import java.util.Arrays;
import org.areslib.command.Command;
import org.areslib.command.CommandScheduler;
import org.areslib.core.AresCommandOpMode;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.coprocessors.OctoMode;
import org.areslib.hardware.interfaces.AresDigitalSensor;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.hardware.wrappers.AresGamepad;
import org.areslib.hardware.wrappers.AresOctoQuadSensor;
import org.areslib.hardware.wrappers.AresSrsSensor;
import org.areslib.hardware.wrappers.CRServoWrapper;
import org.areslib.hardware.wrappers.DcMotorExWrapper;
import org.areslib.hardware.wrappers.PinpointOdometryWrapper;
import org.areslib.hardware.wrappers.SrsMode;
import org.areslib.subsystems.drive.SwerveDriveSubsystem;
import org.areslib.subsystems.drive.SwerveModuleIOReal;
import org.areslib.telemetry.AndroidDashboardBackend;
import org.areslib.telemetry.AresAutoLogger;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.WpiLogBackend;

/**
 * Example Swerve Drive TeleOp Configuration.
 *
 * <p>Hardware Map Architecture: - All Drive Motors are connected to the Main REV Hub (e.g. ports 0
 * to 3) - All Steering Actuators are Continuous Rotation (CR) Servos on the Main REV Hub - All
 * Drive Encoders (quadrature) are routed through the DigitalChickenLabs OctoQuad (Ports 0-3) - All
 * Turn Encoders (REV Through Bore v1 Absolute PWM) are routed through the OctoQuad (Ports 4-7)
 */
@TeleOp(name = "Example: OctoQuad Rev Swerve", group = "ARES Examples")
public class SwerveRevOctoQuadTeleOp extends AresCommandOpMode {

  private SwerveDriveSubsystem driveSubsystem;
  private AresGamepad pilot;
  private double headingOffsetRad = 0.0;

  private PinpointOdometryWrapper pinpoint;
  private OdometryIO.OdometryInputs pinpointInputs = new OdometryIO.OdometryInputs();
  private AresDigitalSensor floodgateSwitch;

  @Override
  public void robotInit() {
    // 1. Initialize Telemetry Backends
    AresTelemetry.registerBackend(new AndroidDashboardBackend());
    AresTelemetry.registerBackend(new WpiLogBackend("/sdcard/FIRST/logs"));

    // 2. Hardware Bulk Caching & Global Hardware Manager Initialization
    // This will automatically find and instantiate the "octoquad" from the FTC Hardware Map
    AresHardwareManager.initHardware(hardwareMap);

    // Map the goBILDA pinpoint hardware (Physically connected to an SRS Hub I2C port)
    pinpoint = new PinpointOdometryWrapper(hardwareMap, "pinpoint");

    // Map the goBILDA floodgate switch (Physically connected to SRS Hub digital port 0)
    floodgateSwitch = new AresSrsSensor(0, SrsMode.DIGITAL);

    org.firstinspires.ftc.teamcode.subsystems.drive.SwerveConfig config =
        new org.firstinspires.ftc.teamcode.subsystems.drive.SwerveConfig();

    // 3. Initialize Swerve Drive Subsystem wrapping mixed hardware seamlessly
    driveSubsystem =
        new SwerveDriveSubsystem(
            config,
            // Front Left Module
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "flDrive")), // Main Hub Motor
                new CRServoWrapper(hardwareMap.get(CRServo.class, "flTurn")), // Main Hub CR Servo
                new AresOctoQuadSensor(0, OctoMode.ENCODER), // OctoQuad Port 0 (Relative)
                new AresOctoQuadSensor(4, OctoMode.ABSOLUTE), // OctoQuad Port 4 (Absolute PWM)
                config.getDriveMetersPerTick()),
            // Front Right Module
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "frDrive")), // Main Hub Motor
                new CRServoWrapper(hardwareMap.get(CRServo.class, "frTurn")), // Main Hub CR Servo
                new AresOctoQuadSensor(1, OctoMode.ENCODER), // OctoQuad Port 1 (Relative)
                new AresOctoQuadSensor(5, OctoMode.ABSOLUTE), // OctoQuad Port 5 (Absolute PWM)
                config.getDriveMetersPerTick()),
            // Back Left Module
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "blDrive")), // Main Hub Motor
                new CRServoWrapper(hardwareMap.get(CRServo.class, "blTurn")), // Main Hub CR Servo
                new AresOctoQuadSensor(2, OctoMode.ENCODER), // OctoQuad Port 2 (Relative)
                new AresOctoQuadSensor(6, OctoMode.ABSOLUTE), // OctoQuad Port 6 (Absolute PWM)
                config.getDriveMetersPerTick()),
            // Back Right Module
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "brDrive")), // Main Hub Motor
                new CRServoWrapper(hardwareMap.get(CRServo.class, "brTurn")), // Main Hub CR Servo
                new AresOctoQuadSensor(3, OctoMode.ENCODER), // OctoQuad Port 3 (Relative)
                new AresOctoQuadSensor(7, OctoMode.ABSOLUTE), // OctoQuad Port 7 (Absolute PWM)
                config.getDriveMetersPerTick()));

    // Register the subsystem so its periodic() functions run in the CommandScheduler
    CommandScheduler.getInstance().registerSubsystem(driveSubsystem);

    // 4. Input Bindings
    pilot = new AresGamepad(gamepad1);

    pilot
        .y()
        .onTrue(
            new Command() {
              @Override
              public void initialize() {
                headingOffsetRad = pinpointInputs.headingRadians;
              }

              @Override
              public boolean isFinished() {
                return true;
              }
            });

    // Simple default command using lambdas for TeleOp control
    CommandScheduler.getInstance()
        .setDefaultCommand(
            driveSubsystem,
            new Command() {
              @Override
              public void execute() {
                // Update and log additional hardware state
                pinpoint.updateInputs(pinpointInputs);
                AresAutoLogger.processInputs("PinpointOdometry", pinpointInputs);
                AresAutoLogger.recordOutput(
                    "Hardware/FloodgateSwitch", floodgateSwitch.getState() ? 1.0 : 0.0);

                driveSubsystem.driveFieldCentric(
                    pilot.getLeftY() * 3.0, // Forward Velocity (m/s)
                    pilot.getLeftX() * 3.0, // Strafe Velocity (m/s)
                    pilot.getRightX() * 3.0, // Angular Velocity (rad/s)
                    new org.areslib.math.geometry.Rotation2d(
                        pinpointInputs.headingRadians - headingOffsetRad));
              }

              @Override
              public java.util.Set<org.areslib.command.Subsystem> getRequirements() {
                // Tells the CommandScheduler that this command monopolizes the driveSubsystem
                return new java.util.HashSet<>(Arrays.asList(driveSubsystem));
              }
            });
  }
}

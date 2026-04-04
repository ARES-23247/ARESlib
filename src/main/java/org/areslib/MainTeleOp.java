package org.areslib;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.areslib.core.AresCommandOpMode;
import org.areslib.command.CommandScheduler;
import org.areslib.command.Command;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.SwerveModuleIOReal;
import org.areslib.hardware.wrappers.AresGamepad;
import org.areslib.hardware.wrappers.DcMotorExWrapper;
import org.areslib.subsystems.drive.DriveSubsystem;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.AndroidDashboardBackend;
import org.areslib.telemetry.WpiLogBackend;

import java.util.Arrays;

@TeleOp(name = "ARES Main TeleOp", group = "ARES")
public class MainTeleOp extends AresCommandOpMode {

    private DriveSubsystem driveSubsystem;
    private AresGamepad pilot;

    @Override
    public void robotInit() {
        // 1. Initialize Telemetry Backends
        AresTelemetry.registerBackend(new AndroidDashboardBackend());
        // Writes native .wpilog formatted objects directly to the robot's onboard flash mapping into AdvantageScope's native layout
        AresTelemetry.registerBackend(new WpiLogBackend("/sdcard/FIRST/logs"));

        // 2. Hardware Bulk Caching Initialization
        AresHardwareManager.initHardware(hardwareMap);

        // 3. Initialize Drive Subsystem wrapping mixed hardware seamlessly
        driveSubsystem = new DriveSubsystem(
            // Front Left
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "flDrive")),
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "flTurn")),
                new org.areslib.hardware.wrappers.OctoQuadEncoderWrapper(hardwareMap, "octoquad", 0),
                new org.areslib.hardware.wrappers.SrsHubEncoderWrapper(hardwareMap, "srshub", 0)
            ),
            // Front Right
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "frDrive")),
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "frTurn")),
                new org.areslib.hardware.wrappers.OctoQuadEncoderWrapper(hardwareMap, "octoquad", 1),
                new org.areslib.hardware.wrappers.SrsHubEncoderWrapper(hardwareMap, "srshub", 1)
            ),
            // Back Left
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "blDrive")),
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "blTurn")),
                new org.areslib.hardware.wrappers.OctoQuadEncoderWrapper(hardwareMap, "octoquad", 2),
                new org.areslib.hardware.wrappers.SrsHubEncoderWrapper(hardwareMap, "srshub", 2)
            ),
            // Back Right
            new SwerveModuleIOReal(
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "brDrive")),
                new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "brTurn")),
                new org.areslib.hardware.wrappers.OctoQuadEncoderWrapper(hardwareMap, "octoquad", 3),
                new org.areslib.hardware.wrappers.SrsHubEncoderWrapper(hardwareMap, "srshub", 3)
            )
        );

        CommandScheduler.getInstance().registerSubsystem(driveSubsystem);

        // 4. Input Bindings
        pilot = new AresGamepad(gamepad1);

        // Simple default command using lambdas
        CommandScheduler.getInstance().setDefaultCommand(driveSubsystem, new Command() {
            @Override
            public void execute() {
                driveSubsystem.drive(
                    pilot.getLeftY() * 3.0,   // Forward
                    pilot.getLeftX() * 3.0,   // Strafe
                    pilot.getRightX() * 2.0   // Turn
                );
            }

            @Override
            public java.util.Set<org.areslib.command.Subsystem> getRequirements() {
                return new java.util.HashSet<>(Arrays.asList(driveSubsystem));
            }
        });
    }
}

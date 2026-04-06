package org.areslib.examples.mecanum;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.areslib.core.AresCommandOpMode;
import org.areslib.command.CommandScheduler;
import org.areslib.command.Command;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.wrappers.AresGamepad;
import org.areslib.hardware.wrappers.DcMotorExWrapper;

import org.areslib.subsystems.drive.MecanumDriveSubsystem;
import org.areslib.subsystems.drive.MecanumDriveIOReal;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.AndroidDashboardBackend;

import org.areslib.hardware.wrappers.PinpointOdometryWrapper;
import org.areslib.hardware.wrappers.DigitalSensorWrapper;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.telemetry.AresAutoLogger;

import java.util.Arrays;

/**
 * Example Mecanum Drive TeleOp Configuration.
 * <p>
 * Hardware Map Architecture:
 * - 4x standard FTC Motors (e.g. goBILDA 5203) plugged directly into the Control/Expansion Hubs.
 * - 4x native motor encoders plugged into the corresponding motor encoder ports.
 * - This represents a completely standard "Rookie Kit" configuration, flawlessly abstracted.
 */
@TeleOp(name = "Example: Standard REV Mecanum", group = "ARES Examples")
public class MecanumStandardTeleOp extends AresCommandOpMode {

    private MecanumDriveSubsystem driveSubsystem;
    private AresGamepad pilot;

    private PinpointOdometryWrapper pinpoint;
    private OdometryIO.OdometryInputs pinpointInputs = new OdometryIO.OdometryInputs();
    private DigitalSensorWrapper floodgateSwitch;

    @Override
    public void robotInit() {
        // 1. Initialize Telemetry Backends
        AresTelemetry.registerBackend(new AndroidDashboardBackend());

        // 2. Hardware Bulk Caching Initialization
        AresHardwareManager.initHardware(hardwareMap);

        // Map the goBILDA pinpoint hardware
        pinpoint = new PinpointOdometryWrapper(hardwareMap, "pinpoint");
        
        // Map the goBILDA floodgate switch
        floodgateSwitch = new DigitalSensorWrapper(hardwareMap.get(DigitalChannel.class, "floodgate"));

        // 3. Initialize Mecanum Drive Subsystem
        DcMotorExWrapper fl = new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "flDrive"));
        DcMotorExWrapper fr = new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "frDrive"));
        DcMotorExWrapper bl = new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "blDrive"));
        DcMotorExWrapper br = new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "brDrive"));

        driveSubsystem = new MecanumDriveSubsystem(
            new MecanumDriveIOReal(
                fl, fr, bl, br,
                fl, fr, bl, br,
                0.001 // example distancePerTick scalar to convert raw ticks to meters
            )
        );

        CommandScheduler.getInstance().registerSubsystem(driveSubsystem);

        // 4. Input Bindings
        pilot = new AresGamepad(gamepad1);

        // Simple default command using lambdas for TeleOp control
        CommandScheduler.getInstance().setDefaultCommand(driveSubsystem, new Command() {
            @Override
            public void execute() {
                // Update and log additional hardware state
                pinpoint.updateInputs(pinpointInputs);
                AresAutoLogger.processInputs("PinpointOdometry", pinpointInputs);
                AresAutoLogger.recordOutput("Hardware/FloodgateSwitch", floodgateSwitch.getState() ? 1.0 : 0.0);

                driveSubsystem.drive(
                    pilot.getLeftY() * 2.5,   // Forward Velocity (m/s)
                    pilot.getLeftX() * 2.5,   // Strafe Velocity (m/s)
                    pilot.getRightX() * 3.0   // Angular Velocity (rad/s)
                );
            }

            @Override
            public java.util.Set<org.areslib.command.Subsystem> getRequirements() {
                return new java.util.HashSet<>(Arrays.asList(driveSubsystem));
            }
        });
    }
}

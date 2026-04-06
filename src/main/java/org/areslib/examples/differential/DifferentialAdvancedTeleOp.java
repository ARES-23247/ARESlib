package org.areslib.examples.differential;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.areslib.core.AresCommandOpMode;
import org.areslib.command.CommandScheduler;
import org.areslib.command.Command;
import org.areslib.hardware.AresHardwareManager;
import org.areslib.hardware.wrappers.AresGamepad;
import org.areslib.hardware.wrappers.DcMotorExWrapper;

import org.areslib.subsystems.drive.DifferentialDriveSubsystem;
import org.areslib.subsystems.drive.DifferentialDriveIOReal;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.AndroidDashboardBackend;
import org.areslib.telemetry.WpiLogBackend;

import org.areslib.hardware.wrappers.PinpointOdometryWrapper;
import org.areslib.hardware.wrappers.DigitalSensorWrapper;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.telemetry.AresAutoLogger;

import java.util.Arrays;

/**
 * Example Differential (Tank) Drive TeleOp Configuration.
 * <p>
 * Hardware Map Architecture:
 * - 2x standard FTC Motors (e.g. goBILDA) driving the left and right sides.
 * - 1x goBILDA Pinpoint Odometry Computer routing independent tracking wheels directly to the IO layer.
 * - Demonstrates how seamlessly high-end parallel coprocessors map natively into standard drive inputs.
 */
@TeleOp(name = "Example: Differential Advanced (Pinpoint)", group = "ARES Examples")
public class DifferentialAdvancedTeleOp extends AresCommandOpMode {

    private DifferentialDriveSubsystem driveSubsystem;
    private AresGamepad pilot;

    private PinpointOdometryWrapper pinpoint;
    private OdometryIO.OdometryInputs pinpointInputs = new OdometryIO.OdometryInputs();
    private DigitalSensorWrapper floodgateSwitch;

    @Override
    public void robotInit() {
        // 1. Initialize Telemetry Backends
        AresTelemetry.registerBackend(new AndroidDashboardBackend());
        AresTelemetry.registerBackend(new WpiLogBackend("/sdcard/FIRST/logs"));

        // 2. Hardware Bulk Caching Initialization
        AresHardwareManager.initHardware(hardwareMap);

        // Map the goBILDA pinpoint hardware
        pinpoint = new PinpointOdometryWrapper(hardwareMap, "pinpoint");
        
        // Map the goBILDA floodgate switch
        floodgateSwitch = new DigitalSensorWrapper(hardwareMap.get(DigitalChannel.class, "floodgate"));

        // 3. Initialize Differential Drive Subsystem
        DcMotorExWrapper left = new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "leftDrive"));
        DcMotorExWrapper right = new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "rightDrive"));
        
        driveSubsystem = new DifferentialDriveSubsystem(
            new DifferentialDriveIOReal(
                left,
                right,
                left,  
                right, 
                0.001 // distance Per Tick
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
                    pilot.getLeftY() * 3.0,   // Forward Velocity (m/s)
                    pilot.getRightX() * 2.5   // Angular Velocity (rad/s)
                );
            }

            @Override
            public java.util.Set<org.areslib.command.Subsystem> getRequirements() {
                return new java.util.HashSet<>(Arrays.asList(driveSubsystem));
            }
        });
    }
}

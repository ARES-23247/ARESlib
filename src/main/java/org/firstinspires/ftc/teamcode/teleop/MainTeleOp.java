package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.areslib.core.AresCommandOpMode;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.AndroidDashboardBackend;
import org.firstinspires.ftc.teamcode.RobotContainer;

/**
 * Advanced Command-Based TeleOp demonstrating multiple subsystems and automated alignment.
 * Built gracefully delegating hardware and bindings to the RobotContainer.
 */
@TeleOp(name = "Team Template: Advanced Command TeleOp", group = "Teamcode")
public class MainTeleOp extends AresCommandOpMode {
    @SuppressWarnings("unused")
    private RobotContainer robot;

    @Override
    public void robotInit() {
        // 1. Initialize Telemetry Backends
        AresTelemetry.registerBackend(new AndroidDashboardBackend());

        // 2. Load the RobotContainer, spawning all hardware abstraction
        robot = new RobotContainer(hardwareMap, gamepad1, gamepad2);
    }
}

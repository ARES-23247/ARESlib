package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.areslib.core.AresCommandOpMode;
import org.areslib.command.CommandScheduler;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.AndroidDashboardBackend;
import org.firstinspires.ftc.teamcode.RobotContainer;

@Autonomous(name = "Auto: Red Left", group = "Competition")
public class AutoRedLeft extends AresCommandOpMode {

    private RobotContainer robot;

    @Override
    public void robotInit() {
        // 1. Telemetry
        AresTelemetry.registerBackend(new AndroidDashboardBackend());

        // 2. Initialize Hardware map state via the core architectural container
        // Pass null gamepads — this is Auto-only, no TeleOp bindings needed
        robot = new RobotContainer(hardwareMap, null, null);

        // 3. Pre-Match Initialization Loop
        // Commonly used for identifying randomized game elements via computer vision
        while (!isStarted() && !isStopRequested()) {
            // Actively poll the camera pipeline while waiting
            robot.getVision().periodic();

            AresTelemetry.putString("Match Info", "Waiting for match start...");
            AresTelemetry.putString("Vision Locked", String.valueOf(robot.getVision().hasTarget()));
            AresTelemetry.putString("Auto Selection", "Red Left Trajectories");
            AresTelemetry.update();
            sleep(20);
        }

        // 4. Match Started: Schedule the specific Red Left autonomous routine!
        CommandScheduler.getInstance().schedule(robot.getRedLeftAutoCommand());
    }
}

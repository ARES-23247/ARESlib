package org.areslib.sim;

import java.util.ArrayList;
import java.util.List;
import org.areslib.command.CommandScheduler;
import org.areslib.subsystems.drive.SwerveModuleIOSim;
import org.areslib.hardware.wrappers.ArrayLidarIOSim;
import org.areslib.subsystems.drive.SwerveDriveSubsystem;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.DesktopLiveBackend;
import org.areslib.telemetry.WpiLogBackend;
import org.areslib.hardware.interfaces.OdometryIO;

import org.areslib.hardware.wrappers.AresGamepad;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;
import com.pedropathing.geometry.Pose;

import org.areslib.sim.games.GameSimulation;
import org.areslib.sim.games.IntoTheDeepSim;
import org.areslib.sim.games.RobotSimState;

public class DesktopSimLauncher {

    public static void main(String[] args) {
        System.out.println("Initializing ARES Simulator Environment...");

        // 1. Register Telemetry Base
        AresTelemetry.registerBackend(new DesktopLiveBackend());
        AresTelemetry.registerBackend(new WpiLogBackend("logs/sim"));
        AresTelemetry.registerBackend(new org.areslib.telemetry.RlogServerBackend(5800));

        org.areslib.subsystems.drive.SwerveConfig config = new org.areslib.subsystems.drive.SwerveConfig();

        // 2. Mock Hardware Layer
        SwerveDriveSubsystem driveSubsystem = new SwerveDriveSubsystem(
            config,
            new SwerveModuleIOSim(),
            new SwerveModuleIOSim(),
            new SwerveModuleIOSim(),
            new SwerveModuleIOSim()
        );

        // Define Custom Spawn Point (Hook into Pedro Paths here)
        Pose startPose = new Pose(0.0, 0.0, 0);

        OdometryIO.OdometryInputs odometryInputs = new OdometryIO.OdometryInputs();
        odometryInputs.xMeters = startPose.getX();
        odometryInputs.yMeters = startPose.getY();
        odometryInputs.headingRadians = startPose.getHeading();

        CommandScheduler.getInstance().registerSubsystem(driveSubsystem);

        // Initialize dyn4j Physics World
        World<Body> world = new World<>();
        world.setGravity(World.ZERO_GRAVITY);

        ArrayLidarIOSim lidarSim = new ArrayLidarIOSim(() -> odometryInputs, world); 
        org.areslib.hardware.interfaces.ArrayLidarIO.ArrayLidarInputs lidarInputs = new org.areslib.hardware.interfaces.ArrayLidarIO.ArrayLidarInputs();

        // Target active game state logic! Easily interchangeable for Centerstage, Into The Deep, etc.
        GameSimulation gameSim = new IntoTheDeepSim();
        gameSim.initField(world);

        // Create Primary Robot Rigid Body (18x18 inches = 0.4572m square)
        Body robotBody = new Body();
        robotBody.addFixture(Geometry.createRectangle(0.4572, 0.4572));
        robotBody.setMass(MassType.NORMAL);
        robotBody.translate(startPose.getX(), startPose.getY());
        robotBody.getTransform().setRotation(startPose.getHeading());
        world.addBody(robotBody);

        // Driver Station GUI Init
        AresDriverStationApp dsApp = new AresDriverStationApp();
        AresGamepad driverGamepad = new AresGamepad(dsApp.getGamepadWrapper().gamepad);
        
        // Form standard list array of active robots for the simulator context
        List<RobotSimState> activeRobots = new ArrayList<>();
        activeRobots.add(new RobotSimState(robotBody, dsApp.getGamepadWrapper().gamepad));

        System.out.println("Sim Started! Connect AdvantageScope to 127.0.0.1");

        // 3. Application Math Core
        try {
            while (true) {
                long startTime = System.currentTimeMillis();

                // 1. Update Gamepad Inputs
                dsApp.getGamepadWrapper().update();

                com.qualcomm.robotcore.hardware.Gamepad gamepad = dsApp.getGamepadWrapper().gamepad;
                dsApp.updateGamepadState(
                    gamepad.left_stick_x,
                    gamepad.left_stick_y,
                    gamepad.right_stick_x,
                    gamepad.right_stick_y,
                    gamepad.left_bumper,
                    gamepad.right_bumper,
                    gamepad.left_trigger,
                    gamepad.right_trigger,
                    gamepad.a,
                    gamepad.b,
                    gamepad.x,
                    gamepad.y
                );
                
                // Process physical game piece interactions natively through the decoupled season class
                gameSim.updateField(world, activeRobots);

                // 2. Fake TeleOp Control Mapping
                double driveY = driverGamepad.getLeftY() * -org.areslib.core.localization.AresPedroConstants.teleOpMaxSpeedForward;
                double driveX = driverGamepad.getLeftX() * -org.areslib.core.localization.AresPedroConstants.teleOpMaxSpeedStrafe;
                double driveTurn = driverGamepad.getRightX() * -org.areslib.core.localization.AresPedroConstants.teleOpMaxTurnRads;
                
                // If triggers are pulled, boost speed
                if (dsApp.getGamepadWrapper().gamepad.right_trigger > 0.5) {
                    driveY *= org.areslib.core.localization.AresPedroConstants.teleOpBoostMultiplier; 
                    driveX *= org.areslib.core.localization.AresPedroConstants.teleOpBoostMultiplier;
                    driveTurn *= org.areslib.core.localization.AresPedroConstants.teleOpBoostMultiplier;
                }
                
                driveSubsystem.drive(driveY, driveX, driveTurn);

                // Scheduler Tick
                CommandScheduler.getInstance().run();

                // Fake Physics Integration (20ms)
                double loopSecs = 0.02;
                double vx = driveSubsystem.getCommandedVx();      // Robot-centric forward (m/s)
                double vy = driveSubsystem.getCommandedVy();      // Robot-centric left (m/s)
                double omega = driveSubsystem.getCommandedOmega(); // rad/s
                
                double currentHeadingRad = odometryInputs.headingRadians;
                
                // Convert to field-centric
                double vXField = vx * Math.cos(currentHeadingRad) - vy * Math.sin(currentHeadingRad);
                double vYField = vx * Math.sin(currentHeadingRad) + vy * Math.cos(currentHeadingRad);
                
                // Integrate with dyn4j
                robotBody.setLinearVelocity(new Vector2(vXField, vYField));
                robotBody.setAngularVelocity(omega);
                robotBody.setAtRest(false); // Force awaken the body so dyn4j integrates the velocity
                
                world.update(loopSecs);

                // Fetch resulting poses confined to the rigid body constraint simulation
                odometryInputs.xMeters = robotBody.getTransform().getTranslationX();
                odometryInputs.yMeters = robotBody.getTransform().getTranslationY();
                odometryInputs.headingRadians = robotBody.getTransform().getRotationAngle();

                // Lidar Update
                lidarSim.updateInputs(lidarInputs);
                org.areslib.telemetry.AresAutoLogger.processInputs("Sensors/LiDAR", lidarInputs);
                org.areslib.telemetry.AresAutoLogger.processInputs("Pedro/Odometry", odometryInputs);
                
                // Publish Pose2d
                AresTelemetry.putPose2d("Robot/Pose", 
                    odometryInputs.xMeters, 
                    odometryInputs.yMeters, 
                    odometryInputs.headingRadians 
                );

                // Push Field States
                gameSim.telemetryUpdate();

                // Telemetry Event Push
                AresTelemetry.update();
                dsApp.updateHud(odometryInputs.xMeters, odometryInputs.yMeters, odometryInputs.headingRadians, 0); // Removed heldSamples from core HUD payload, its game-specific

                // Precise 50Hz sleep
                long loopTime = System.currentTimeMillis() - startTime;
                if (loopTime < 20) {
                    Thread.sleep(20 - loopTime);
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Simulation Faulted: " + e.getMessage());
        }
    }
}

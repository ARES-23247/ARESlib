package org.areslib.sim;

import java.util.ArrayList;
import java.util.List;
import org.areslib.command.CommandScheduler;
import org.areslib.core.FieldConstants;
import org.areslib.subsystems.drive.AresDrivetrain;
import org.firstinspires.ftc.teamcode.RobotContainer;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.DesktopLiveBackend;
import org.areslib.telemetry.WpiLogBackend;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.pathplanner.commands.PathPlannerAuto;

import org.areslib.hardware.wrappers.AresGamepad;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import org.areslib.sim.games.GameSimulation;
import org.areslib.sim.games.IntoTheDeepSim;
import org.areslib.sim.games.RobotSimState;

public class DesktopSimLauncher {

    public static void main(String[] args) {
        System.out.println("Initializing ARES Simulator Environment...");
        org.areslib.core.AresRobot.setSimulation(true);

        // 1. Register Telemetry Base
        AresTelemetry.registerBackend(new DesktopLiveBackend());
        AresTelemetry.registerBackend(new WpiLogBackend("logs/sim"));
        AresTelemetry.registerBackend(new org.areslib.telemetry.RlogServerBackend(5800));

        // 2. Mock Hardware Layer (Via RobotContainer)
        // Since isSimulation is true, hardwareMap and gamepads can be safely passed as null or mock values
        RobotContainer robotContainer = new RobotContainer(null, null, null);
        AresDrivetrain driveSubsystem = robotContainer.getDrivetrain();

        // Define Custom Spawn Point
        // Physical origin is standard (0,0) in dyn4j/AdvantageScope
        OdometryIO.OdometryInputs odometryInputs = robotContainer.getOdometryInputs();
        odometryInputs.xMeters = 0.0;
        odometryInputs.yMeters = 0.0;
        odometryInputs.headingRadians = 0.0;

        // Initialize dyn4j Physics World
        World<Body> world = new World<>();
        world.setGravity(World.ZERO_GRAVITY);

        org.areslib.hardware.wrappers.ArrayLidarIOSim lidarSim = new org.areslib.hardware.wrappers.ArrayLidarIOSim(() -> odometryInputs, world); 
        org.areslib.hardware.interfaces.ArrayLidarIO.ArrayLidarInputs lidarInputs = new org.areslib.hardware.interfaces.ArrayLidarIO.ArrayLidarInputs();

        // Target active game state logic! Easily interchangeable for Centerstage, Into The Deep, etc.
        GameSimulation gameSim = new IntoTheDeepSim();
        gameSim.initField(world);

        // Create Primary Robot Rigid Body using named constants from FieldConstants
        Body robotBody = new Body();
        org.dyn4j.dynamics.BodyFixture fixture = new org.dyn4j.dynamics.BodyFixture(
            Geometry.createRectangle(FieldConstants.ROBOT_SIZE_METERS, FieldConstants.ROBOT_SIZE_METERS)
        );
        double area = FieldConstants.ROBOT_SIZE_METERS * FieldConstants.ROBOT_SIZE_METERS;
        // Automatically scale dyn4j mass to match robot characterization
        fixture.setDensity(12.0 / area);
        fixture.setFriction(FieldConstants.SIM_WALL_FRICTION);
        fixture.setRestitution(FieldConstants.SIM_WALL_RESTITUTION);
        robotBody.addFixture(fixture);
        
        robotBody.setMass(MassType.NORMAL);
        
        // Synthetic floor carpet friction metrics
        robotBody.setLinearDamping(FieldConstants.SIM_LINEAR_DAMPING);
        robotBody.setAngularDamping(FieldConstants.SIM_ANGULAR_DAMPING);
        robotBody.translate(0.0, 0.0);
        robotBody.getTransform().setRotation(0.0);
        world.addBody(robotBody);

        // Driver Station GUI Init
        AresDriverStationApp dsApp = new AresDriverStationApp();
        AresGamepad driverGamepad = new AresGamepad(dsApp.getGamepadWrapper().gamepad);
        
        // Form standard list array of active robots for the simulator context
        List<RobotSimState> activeRobots = new ArrayList<>();
        activeRobots.add(new RobotSimState(robotBody, dsApp.getGamepadWrapper().gamepad));

        System.out.println("Sim Started! Connect AdvantageScope to 127.0.0.1");

        boolean wasAutoEnabled = false;

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

                boolean isAutoEnabled = dsApp.isAutoModeEnabled();
                if (isAutoEnabled && !wasAutoEnabled) {
                    // 6. Hook Teleop or Auto Commands
                    try {
                        CommandScheduler.getInstance().schedule(new PathPlannerAuto("SquareAuto"));
                    } catch (Exception autoEx) {
                        System.err.println("[Sim] Auto failed to load: " + autoEx.getMessage());
                        autoEx.printStackTrace();
                    }
                } else if (!isAutoEnabled && wasAutoEnabled) {
                    CommandScheduler.getInstance().cancelAll();
                }
                wasAutoEnabled = isAutoEnabled;

                // 2. TeleOp Control Mapping
                if (!isAutoEnabled) {
                    double driveY = driverGamepad.getLeftY() * 1.0; // +X is Forward
                    double driveX = driverGamepad.getLeftX() * -1.0; // +Y is Left
                    double driveTurn = driverGamepad.getRightX() * -3.14; // +Theta is CCW
                    
                    // If triggers are pulled, boost speed
                    if (dsApp.getGamepadWrapper().gamepad.right_trigger > 0.5) {
                        driveY *= 1.5; 
                        driveX *= 1.5;
                        driveTurn *= 1.5;
                    }
                    
                    // Apply alliance-specific heading offset for field-centric driving.
                    // RED alliance: offset = 0° (driver faces +X, forward = away from driver).
                    // BLUE alliance: offset = 180° (driver faces -X, forward = away from driver).
                    // The offset rotates the robot's heading reference so that "push stick forward"
                    // always drives the robot AWAY from the driver regardless of which side they sit on.
                    FieldConstants.Alliance currentAlliance = dsApp.getAlliance();
                    org.areslib.math.geometry.Rotation2d currentHeading = new org.areslib.math.geometry.Rotation2d(odometryInputs.headingRadians);
                    org.areslib.math.geometry.Rotation2d allianceHeading = currentHeading.rotateBy(
                        new org.areslib.math.geometry.Rotation2d(currentAlliance.getHeadingOffsetRadians())
                    );

                    if (driveSubsystem instanceof org.areslib.subsystems.drive.SwerveDriveSubsystem) {
                        ((org.areslib.subsystems.drive.SwerveDriveSubsystem) driveSubsystem).driveFieldCentric(driveY, driveX, driveTurn, allianceHeading);
                    } else if (driveSubsystem instanceof org.areslib.subsystems.drive.MecanumDriveSubsystem) {
                        ((org.areslib.subsystems.drive.MecanumDriveSubsystem) driveSubsystem).driveFieldCentric(driveY, driveX, driveTurn, allianceHeading);
                    } else if (driveSubsystem instanceof org.areslib.subsystems.drive.DifferentialDriveSubsystem) {
                        ((org.areslib.subsystems.drive.DifferentialDriveSubsystem) driveSubsystem).drive(driveY, driveTurn);
                    }
                }

                // Scheduler Tick
                CommandScheduler.getInstance().run();

                double loopSecs = org.areslib.core.AresRobot.LOOP_PERIOD_SECS;
                double vx = driveSubsystem.getCommandedVx();      // Robot-centric forward (m/s)
                double vy = driveSubsystem.getCommandedVy();      // Robot-centric left (m/s)
                double omega = driveSubsystem.getCommandedOmega(); // rad/s
                
                // Debug: log commanded speeds during auto
                if (isAutoEnabled && (Math.abs(vx) > 0.01 || Math.abs(vy) > 0.01)) {
                    AresTelemetry.putNumber("Auto/CommandedVx", vx);
                    AresTelemetry.putNumber("Auto/CommandedVy", vy);
                    AresTelemetry.putNumber("Auto/CommandedOmega", omega);
                }

                double currentHeadingRad = odometryInputs.headingRadians;
                
                // Convert to field-centric
                double vXFieldTarget = vx * Math.cos(currentHeadingRad) - vy * Math.sin(currentHeadingRad);
                double vYFieldTarget = vx * Math.sin(currentHeadingRad) + vy * Math.cos(currentHeadingRad);
                
                // Get current physics reality
                double currentVx = robotBody.getLinearVelocity().x;
                double currentVy = robotBody.getLinearVelocity().y;
                double currentOmega = robotBody.getAngularVelocity();

                // Dynamic Grip Constants (Traction Coefficient)
                double driveTractionGrip = 5.0;
                double turnTractionGrip = 5.0;
                
                // Calculate closed-loop synthetic traction forces
                double forceX = (vXFieldTarget - currentVx) * driveTractionGrip * robotBody.getMass().getMass();
                double forceY = (vYFieldTarget - currentVy) * driveTractionGrip * robotBody.getMass().getMass();
                double torque = (omega - currentOmega) * turnTractionGrip * robotBody.getMass().getInertia();
                
                // Apply physics
                robotBody.applyForce(new Vector2(forceX, forceY));
                robotBody.applyTorque(torque);
                robotBody.setAtRest(false); // Force awaken the body so dyn4j integrates the velocity
                
                world.update(loopSecs);

                // Fetch resulting poses confined to the rigid body constraint simulation
                odometryInputs.xMeters = robotBody.getTransform().getTranslationX();
                odometryInputs.yMeters = robotBody.getTransform().getTranslationY();
                odometryInputs.headingRadians = robotBody.getTransform().getRotationAngle();
                
                // Inject velocities so PathPlanner's controllers and feedforwards function properly
                odometryInputs.xVelocityMetersPerSecond = robotBody.getLinearVelocity().x;
                odometryInputs.yVelocityMetersPerSecond = robotBody.getLinearVelocity().y;
                odometryInputs.angularVelocityRadiansPerSecond = robotBody.getAngularVelocity();

                // Lidar Update
                lidarSim.updateInputs(lidarInputs);
                org.areslib.telemetry.AresAutoLogger.processInputs("Sensors/LiDAR", lidarInputs);
                org.areslib.telemetry.AresAutoLogger.processInputs("Drive/Odometry", odometryInputs);
                
                // Publish true physics state as the main robot
                AresTelemetry.putPose2d("Robot/Pose", 
                    odometryInputs.xMeters, 
                    odometryInputs.yMeters, 
                    odometryInputs.headingRadians
                );

                AresTelemetry.putPose2d("PathPlanner/EstimatedPose", 
                    odometryInputs.xMeters + 1.8288, 
                    odometryInputs.yMeters + 1.8288, 
                    odometryInputs.headingRadians);

                // Log current alliance for telemetry
                AresTelemetry.putString("DriverStation/Alliance", dsApp.getAlliance().name());

                // Push Field States
                gameSim.telemetryUpdate();

                // Telemetry Event Push
                AresTelemetry.update();
                int heldSamplesCount = gameSim.getHeldSamples(activeRobots.get(0));
                dsApp.updateHud(odometryInputs.xMeters, odometryInputs.yMeters, odometryInputs.headingRadians, heldSamplesCount);

                // Precise 50Hz sleep
                long loopTime = System.currentTimeMillis() - startTime;
                if (loopTime < 20) {
                    Thread.sleep(20 - loopTime);
                }
            }
        } catch (Exception e) {
            System.err.println("Simulation Faulted: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

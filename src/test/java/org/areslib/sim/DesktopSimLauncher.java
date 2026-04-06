package org.areslib.sim;

import java.util.ArrayList;
import java.util.List;
import org.areslib.command.CommandScheduler;
import org.areslib.subsystems.drive.SwerveModuleIOSim;
import org.areslib.hardware.wrappers.ArrayLidarIOSim;
import org.areslib.subsystems.drive.DriveSubsystem;
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
public class DesktopSimLauncher {

    public static void main(String[] args) {
        System.out.println("Initializing ARES Simulator Environment...");

        // 1. Register Telemetry Base
        AresTelemetry.registerBackend(new DesktopLiveBackend());
        AresTelemetry.registerBackend(new WpiLogBackend("logs/sim"));
        AresTelemetry.registerBackend(new org.areslib.telemetry.RlogServerBackend(5800));

        // 2. Mock Hardware Layer
        DriveSubsystem driveSubsystem = new DriveSubsystem(
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

        // Add 4 static walls representing the 144x144" FTC field.
        // AdvantageScope expects (0,0) at the center. X/Y span [-1.8288, 1.8288]
        double fieldSize = 3.6576;
        double halfField = fieldSize / 2.0;
        double wallThick = 0.1;

        Body leftWall = new Body();
        leftWall.addFixture(Geometry.createRectangle(wallThick, fieldSize + wallThick*2));
        leftWall.translate(-halfField - wallThick/2.0, 0);
        leftWall.setMass(MassType.INFINITE);
        world.addBody(leftWall);

        Body rightWall = new Body();
        rightWall.addFixture(Geometry.createRectangle(wallThick, fieldSize + wallThick*2));
        rightWall.translate(halfField + wallThick/2.0, 0);
        rightWall.setMass(MassType.INFINITE);
        world.addBody(rightWall);

        Body bottomWall = new Body();
        bottomWall.addFixture(Geometry.createRectangle(fieldSize + wallThick*2, wallThick));
        bottomWall.translate(0, -halfField - wallThick/2.0);
        bottomWall.setMass(MassType.INFINITE);
        world.addBody(bottomWall);

        Body topWall = new Body();
        topWall.addFixture(Geometry.createRectangle(fieldSize + wallThick*2, wallThick));
        topWall.translate(0, halfField + wallThick/2.0);
        topWall.setMass(MassType.INFINITE);
        world.addBody(topWall);

        // Create Robot Rigid Body (18x18 inches = 0.4572m square)
        Body robotBody = new Body();
        robotBody.addFixture(Geometry.createRectangle(0.4572, 0.4572));
        robotBody.setMass(MassType.NORMAL);
        robotBody.translate(startPose.getX(), startPose.getY());
        robotBody.getTransform().setRotation(startPose.getHeading());
        world.addBody(robotBody);

        // Spawn interactive Game Pieces (Samples)
        List<Body> gamePieces = new ArrayList<>();
        double[][] sampleSpawns = {
            {0.5, 0.5}, {0.5, 0.0}, {0.5, -0.5}
        };
        for (double[] spawn : sampleSpawns) {
            Body sample = new Body();
            sample.addFixture(Geometry.createRectangle(0.0889, 0.0381)); // 3.5" x 1.5"
            sample.setMass(MassType.NORMAL);
            sample.translate(spawn[0], spawn[1]);
            world.addBody(sample);
            gamePieces.add(sample);
        }

        // Driver Station GUI Init
        AresDriverStationApp dsApp = new AresDriverStationApp();
        AresGamepad driverGamepad = new AresGamepad(dsApp.getGamepadWrapper().gamepad);

        System.out.println("Sim Started! Connect AdvantageScope to 127.0.0.1");

        // 3. Application Math Core
        int heldSamples = 0;
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

                boolean intakeBtn = gamepad.right_bumper;
                boolean outtakeBtn = gamepad.left_bumper;
                
                double rx = robotBody.getTransform().getTranslationX();
                double ry = robotBody.getTransform().getTranslationY();
                double theta = robotBody.getTransform().getRotationAngle();
                
                double intakePointX = rx + Math.cos(theta) * 0.25;
                double intakePointY = ry + Math.sin(theta) * 0.25;
                
                if (intakeBtn) {
                    Body toRemove = null;
                    for (Body piece : gamePieces) {
                        double dx = piece.getTransform().getTranslationX() - intakePointX;
                        double dy = piece.getTransform().getTranslationY() - intakePointY;
                        if (Math.sqrt(dx*dx + dy*dy) <= 0.15) {
                            toRemove = piece;
                            break;
                        }
                    }
                    if (toRemove != null) {
                        gamePieces.remove(toRemove);
                        world.removeBody(toRemove);
                        heldSamples++;
                    }
                }
                
                if (outtakeBtn && heldSamples > 0) {
                    Body newPiece = new Body();
                    newPiece.addFixture(Geometry.createRectangle(0.0889, 0.0381));
                    newPiece.setMass(MassType.NORMAL);
                    newPiece.translate(intakePointX, intakePointY);
                    newPiece.getTransform().setRotation(theta);
                    world.addBody(newPiece);
                    gamePieces.add(newPiece);
                    heldSamples--;
                }
                
                AresTelemetry.putNumber("Robot/HeldSamples", heldSamples);

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

                // Fake Physics Integration (20ms loop)
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
                
                // Publish Pose2d using the modern struct format to avoid deprecation warnings
                AresTelemetry.putPose2d("Robot/Pose", 
                    odometryInputs.xMeters, 
                    odometryInputs.yMeters, 
                    odometryInputs.headingRadians 
                );

                // Fetch Game Piece Coordinates
                double[] gamePieceArray = new double[gamePieces.size() * 7];
                for (int i = 0; i < gamePieces.size(); i++) {
                    Body piece = gamePieces.get(i);
                    gamePieceArray[i * 7] = piece.getTransform().getTranslationX();
                    gamePieceArray[i * 7 + 1] = piece.getTransform().getTranslationY();
                    gamePieceArray[i * 7 + 2] = 0.019; // 0.75 inches up (half height of 1.5in block)
                    
                    double t = piece.getTransform().getRotationAngle();
                    gamePieceArray[i * 7 + 3] = Math.cos(t / 2.0); // qw
                    gamePieceArray[i * 7 + 4] = 0; // qx
                    gamePieceArray[i * 7 + 5] = 0; // qy
                    gamePieceArray[i * 7 + 6] = Math.sin(t / 2.0); // qz
                }
                AresTelemetry.putNumberArray("Field/Samples", gamePieceArray);
                AresTelemetry.putNumberArray("Debug/IntakePoint", new double[]{intakePointX, intakePointY, 0.0, 1.0, 0, 0, 0});

                // Telemetry Event Push
                AresTelemetry.update();
                dsApp.updateHud(odometryInputs.xMeters, odometryInputs.yMeters, odometryInputs.headingRadians, heldSamples);

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

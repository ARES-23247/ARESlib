package org.areslib.sim;

import org.areslib.command.CommandScheduler;
import org.areslib.hardware.SwerveModuleIOSim;
import org.areslib.hardware.wrappers.ArrayLidarIOSim;
import org.areslib.subsystems.drive.DriveSubsystem;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.DesktopLiveBackend;
import org.areslib.telemetry.WpiLogBackend;
import org.areslib.hardware.interfaces.OdometryIO;
import org.areslib.core.localization.AresFollower;
import org.areslib.command.FollowPathCommand;
import com.pedropathing.follower.Follower;
import com.pedropathing.paths.Path;
import com.pedropathing.geometry.BezierLine;
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

        ArrayLidarIOSim lidarSim = new ArrayLidarIOSim(); 
        org.areslib.hardware.sensors.ArrayLidarIO.ArrayLidarInputs lidarInputs = new org.areslib.hardware.sensors.ArrayLidarIO.ArrayLidarInputs();

        CommandScheduler.getInstance().registerSubsystem(driveSubsystem);
        
        OdometryIO.OdometryInputs odometryInputs = new OdometryIO.OdometryInputs();
        AresFollower aresFollower = new AresFollower(driveSubsystem, odometryInputs);
        CommandScheduler.getInstance().registerSubsystem(aresFollower);

        Path testPath = new Path(new BezierLine(
            new Pose(0, 0, 0), 
            new Pose(20, 20, 0)
        ));
        FollowPathCommand followCmd = new FollowPathCommand(aresFollower, testPath);
        CommandScheduler.getInstance().schedule(followCmd);

        System.out.println("Sim Started! Connect AdvantageScope to 127.0.0.1");

        // 3. Application Math Core
        try {
            while (true) {
                long startTime = System.currentTimeMillis();

                // Fake Inputs
                // driveSubsystem.drive(0.5, 0.0, 0.2); // Pedro is driving now!

                // Scheduler Tick
                CommandScheduler.getInstance().run();

                // Fake Physics Integration (20ms loop)
                double loopSecs = 0.02;
                double vx = driveSubsystem.getCommandedVx();      // Robot-centric forward (m/s)
                double vy = driveSubsystem.getCommandedVy();      // Robot-centric left (m/s)
                double omega = driveSubsystem.getCommandedOmega(); // rad/s
                
                double currentHeadingRad = Math.toRadians(odometryInputs.headingDegrees);
                
                // Convert to field-centric
                double vXField = vx * Math.cos(currentHeadingRad) - vy * Math.sin(currentHeadingRad);
                double vYField = vx * Math.sin(currentHeadingRad) + vy * Math.cos(currentHeadingRad);
                
                // Integrate
                odometryInputs.headingDegrees += Math.toDegrees(omega * loopSecs);
                odometryInputs.xInches += (vXField * loopSecs) * (100.0 / 2.54);
                odometryInputs.yInches += (vYField * loopSecs) * (100.0 / 2.54);

                // Lidar Update
                lidarSim.updateInputs(lidarInputs);
                org.areslib.telemetry.AresAutoLogger.processInputs("Sensors/LiDAR", lidarInputs);
                org.areslib.telemetry.AresAutoLogger.processInputs("Pedro/Odometry", odometryInputs);
                
                // Publish Pose2d double array for AdvantageScope Field2d support
                // AdvantageScope expects: double[] { xMeters, yMeters, rotationRadians }
                AresTelemetry.getInstance().putNumberArray("Robot/Pose", new double[] { 
                    odometryInputs.xInches * 0.0254, 
                    odometryInputs.yInches * 0.0254, 
                    Math.toRadians(odometryInputs.headingDegrees) 
                });

                // Telemetry Event Push
                AresTelemetry.update();

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

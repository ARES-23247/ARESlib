package org.areslib;

import org.areslib.core.AresCommandOpMode;
import org.areslib.subsystems.drive.SwerveModuleIOReal;
import org.areslib.hardware.interfaces.AresEncoder;
import org.areslib.hardware.interfaces.AresMotor;
import org.areslib.subsystems.drive.DriveSubsystem;
import org.areslib.telemetry.AndroidDashboardBackend;
import org.areslib.telemetry.AresTelemetry;
import org.areslib.telemetry.WpiLogBackend;

public class RobotAresOpMode extends AresCommandOpMode {

    // Subsystems initialized directly inside robotInit are perfectly fine without being retained as fields.

    @Override
    public void robotInit() {
        // Broadcast telemetry directly to the local FTC Control Hub websocket (192.168.43.1:8081)
        AresTelemetry.registerBackend(new AndroidDashboardBackend());
        
        // Simultaneously log standard .wpilog files natively leveraging pure java.nio buffers without crashing from missing WPILib HAL/JNI files
        AresTelemetry.registerBackend(new WpiLogBackend("/sdcard/FIRST/logs/"));

        // Instantiate a mock motor & encoder functional interface for the base hardware layout 
        // In reality, users inject RevEncoderWrapper, AnalogAbsoluteEncoderWrapper, etc mapped to the FTC SDK's hardwareMap
        AresMotor placeholderMotor = new AresMotor() {
            @Override public void setVoltage(double volts) {}
            @Override public double getVoltage() { return 0.0; }
            @Override public void setCurrentPolling(boolean enabled) {}
            @Override public double getCurrentAmps() { return 0.0; }
        };
        AresEncoder placeholderEncoder = new AresEncoder() {
            @Override public void setDistancePerPulse(double distance) {}
            @Override public double getPosition() { return 0.0; }
            @Override public double getVelocity() { return 0.0; }
        };
        org.areslib.hardware.interfaces.AresAbsoluteEncoder placeholderAbsolute = new org.areslib.hardware.interfaces.AresAbsoluteEncoder() {
            @Override public void setDistancePerPulse(double distance) {}
            @Override public double getPosition() { return 0.0; }
            @Override public double getVelocity() { return 0.0; }
            @Override public double getAbsolutePositionRad() { return 0.0; }
            @Override public void setOffset(double offsetRad) {}
        };

        SwerveModuleIOReal realModule = new SwerveModuleIOReal(
                placeholderMotor, placeholderMotor, 
                placeholderEncoder, placeholderAbsolute
        );

        new DriveSubsystem(
                realModule, realModule, 
                realModule, realModule
        );
    }
}

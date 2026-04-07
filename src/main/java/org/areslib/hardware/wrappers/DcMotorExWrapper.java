package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import org.areslib.hardware.interfaces.AresEncoder;
import org.areslib.hardware.interfaces.AresMotor;

/**
 * Standard implementation mapping FTC Hardware directly to internal physics logic.
 * Enforces native loop optimization via write-caching threshold (0.005 limits).
 */
public class DcMotorExWrapper implements AresMotor, AresEncoder {
    
    private final DcMotorEx dcMotorEx;
    private double lastSentPower = Double.NaN;
    private static final double CACHE_THRESHOLD = 0.005;
    private boolean currentPollingEnabled = false;

    public DcMotorExWrapper(DcMotorEx dcMotorEx) {
        this.dcMotorEx = dcMotorEx;
    }

    @Override
    public void setVoltage(double volts) {
        double currentBattery = Math.max(1.0, org.areslib.hardware.AresHardwareManager.getBatteryVoltage());
        double masterScale = org.areslib.hardware.AresHardwareManager.masterPowerScale;
        double power = (volts / currentBattery) * masterScale;
        
        // Strict boundary scaling
        power = Math.max(-1.0, Math.min(1.0, power));
        
        // Write-Caching block saves massive RS-485 cycle time
        if (Double.isNaN(lastSentPower) || Math.abs(power - lastSentPower) > CACHE_THRESHOLD) {
            dcMotorEx.setPower(power);
            lastSentPower = power;
        }
    }

    @Override
    public double getVoltage() {
        return (Double.isNaN(lastSentPower) ? 0.0 : lastSentPower) * org.areslib.hardware.AresHardwareManager.getBatteryVoltage();
    }

    @Override
    public void setDistancePerPulse(double distance) {}

    @Override
    public void setCurrentPolling(boolean enabled) {
        this.currentPollingEnabled = enabled;
    }

    @Override
    public double getCurrentAmps() {
        if (currentPollingEnabled) {
            return dcMotorEx.getCurrent(org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit.AMPS);
        }
        return 0.0;
    }

    @Override
    public double getPosition() {
        // Leverages native FTC Expansion Hub bulk caching underneath
        return dcMotorEx.getCurrentPosition();
    }

    @Override
    public double getVelocity() {
        return dcMotorEx.getVelocity();
    }
}

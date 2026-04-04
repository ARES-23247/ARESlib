package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.CRServo;
import org.areslib.hardware.interfaces.AresMotor;

public class CRServoWrapper implements AresMotor {
    private final CRServo servo;
    private double lastSentPower = Double.NaN;
    private static final double CACHE_THRESHOLD = 0.005;

    public CRServoWrapper(CRServo servo) {
        this.servo = servo;
    }

    @Override
    public void setVoltage(double volts) {
        // Map voltage (-Battery to +Battery) down to CR Servo power (-1.0 to 1.0)
        double currentBattery = org.areslib.hardware.AresHardwareManager.getBatteryVoltage();
        double power = volts / currentBattery;
        power = Math.max(-1.0, Math.min(1.0, power));
        
        if (Double.isNaN(lastSentPower) || Math.abs(power - lastSentPower) > CACHE_THRESHOLD) {
            servo.setPower(power);
            lastSentPower = power;
        }
    }

    @Override
    public double getVoltage() {
        return (Double.isNaN(lastSentPower) ? 0.0 : lastSentPower) * org.areslib.hardware.AresHardwareManager.getBatteryVoltage();
    }

    @Override
    public void setCurrentPolling(boolean enabled) {}

    @Override
    public double getCurrentAmps() { return 0.0; }
}

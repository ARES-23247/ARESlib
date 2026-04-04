package org.areslib.examples.elevator;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.areslib.hardware.wrappers.DcMotorExWrapper;
import org.areslib.hardware.wrappers.ServoWrapper;

public class ElevatorIOReal implements ElevatorIO {
    
    // We instantiate native Ares wrappers locally. Write-Caching happens completely transparently!
    private final DcMotorExWrapper motor;
    private final ServoWrapper grabberServo;

    private static final double METERS_PER_TICK = 0.001; // Example scale

    public ElevatorIOReal(HardwareMap hardwareMap) {
        // Automatically injects hardware directly into our caching wrappers
        this.motor = new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "elevator_motor"));
        this.grabberServo = new ServoWrapper(hardwareMap.get(Servo.class, "grabber_servo"));
    }

    @Override
    public void updateInputs(ElevatorInputs inputs) {
        inputs.positionMeters = motor.getPosition() * METERS_PER_TICK;
        inputs.velocityMps = motor.getVelocity() * METERS_PER_TICK;
        inputs.appliedVolts = motor.getVoltage();
        inputs.currentAmps = motor.getCurrentAmps();
    }

    @Override
    public void setVoltage(double volts) {
        // Natively scaled voltage and cached execution handled by the wrapper! 
        motor.setVoltage(volts);
    }

    @Override
    public void setGrabberServo(double position) {
        grabberServo.setPosition(position);
    }

    @Override
    public void setCurrentPolling(boolean enabled) {
        motor.setCurrentPolling(enabled);
    }

    @Override
    public void resetEncoder() {
        // Assume native motor reset via mode change (simplified here or requires mapping to STOP_AND_RESET_ENCODER)
        motor.getVelocity(); // DUMMY implementation if not natively exposed in AresMotor
    }
}

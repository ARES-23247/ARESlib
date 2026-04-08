package org.areslib.teamcode.elevator;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import org.areslib.hardware.wrappers.DcMotorExWrapper;
import org.areslib.hardware.wrappers.ServoWrapper;
import org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorConfig;

/**
 * Real hardware implementation of the ElevatorIO interface. Uses Ares wrappers for cached
 * execution.
 */
public class ElevatorIOReal implements ElevatorIO {

  // We instantiate native Ares wrappers locally. Write-Caching happens completely transparently!
  private final DcMotorExWrapper motor;
  private final ServoWrapper grabberServo;

  private final ElevatorConfig config;

  /**
   * Constructs a real elevator hardware IO instance.
   *
   * @param hardwareMap The hardware map to retrieve devices from.
   * @param config The hardware configuration.
   */
  public ElevatorIOReal(HardwareMap hardwareMap, ElevatorConfig config) {
    this.config = config;
    // Automatically injects hardware directly into our caching wrappers
    this.motor = new DcMotorExWrapper(hardwareMap.get(DcMotorEx.class, "elevator_motor"));
    this.grabberServo = new ServoWrapper(hardwareMap.get(Servo.class, "grabber_servo"));
  }

  @Override
  public void updateInputs(ElevatorInputs inputs) {
    inputs.positionMeters = motor.getPosition() * config.getMetersPerTick();
    inputs.velocityMps = motor.getVelocity() * config.getMetersPerTick();
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
    // Assume native motor reset via mode change (simplified here or requires mapping to
    // STOP_AND_RESET_ENCODER)
    motor.getVelocity(); // DUMMY implementation if not natively exposed in AresMotor
  }
}

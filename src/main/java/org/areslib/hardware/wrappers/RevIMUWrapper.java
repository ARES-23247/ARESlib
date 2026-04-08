package org.areslib.hardware.wrappers;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import org.areslib.hardware.interfaces.IMUIO;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

public class RevIMUWrapper implements IMUIO {

  private final IMU imu;

  public RevIMUWrapper(HardwareMap hardwareMap, String deviceName) {
    this.imu = hardwareMap.get(IMU.class, deviceName);
  }

  /**
   * Optional secondary constructor if creating with specific IMU parameters.
   *
   * @param hardwareMap the hardware map to use
   * @param deviceName the name of the IMU on the robot configuration
   * @param parameters the initialization parameters for the IMU
   */
  public RevIMUWrapper(HardwareMap hardwareMap, String deviceName, IMU.Parameters parameters) {
    this.imu = hardwareMap.get(IMU.class, deviceName);
    this.imu.initialize(parameters);
  }

  @Override
  public void updateInputs(IMUInputs inputs) {
    if (imu == null) {
      inputs.connected = false;
      return;
    }

    inputs.connected = true;

    YawPitchRollAngles angles = imu.getRobotYawPitchRollAngles();
    inputs.yawRadians = angles.getYaw(AngleUnit.RADIANS);
    inputs.pitchRadians = angles.getPitch(AngleUnit.RADIANS);
    inputs.rollRadians = angles.getRoll(AngleUnit.RADIANS);

    AngularVelocity velocity = imu.getRobotAngularVelocity(AngleUnit.RADIANS);
    inputs.yawVelocityRadPerSec = velocity.zRotationRate;
    inputs.pitchVelocityRadPerSec = velocity.yRotationRate;
    inputs.rollVelocityRadPerSec = velocity.xRotationRate;
  }

  @Override
  public void resetYaw() {
    if (imu != null) {
      imu.resetYaw();
    }
  }
}

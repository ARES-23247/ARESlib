package org.firstinspires.ftc.teamcode;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide numerical or boolean
 * constants. This class should not be used for any other purpose. All constants should be declared
 * globally (i.e. public static). Do not put anything functional in this class.
 *
 * <p>It is advised to statically import this class (or one of its inner classes) wherever the
 * constants are needed, to reduce verbosity.
 */
public final class Constants {

  public static final class ElevatorConstants {
    // Standard PID gains
    public static final double P = 50.0;
    public static final double G = 0.2; // Gravity feedforward

    // Software limits
    public static final double MAX_POSITION_METERS = 1.0;
    public static final double MIN_POSITION_METERS = 0.0;

    // Target Heights
    public static final double HIGH_POSITION_METERS = 0.8;
    public static final double LOW_POSITION_METERS = 0.0;

    public static final org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorConfig
        ELEVATOR_CONFIG = new org.firstinspires.ftc.teamcode.subsystems.elevator.ElevatorConfig();

    static {
      ELEVATOR_CONFIG.spoolDiameterMM = 38.0;
      ELEVATOR_CONFIG.motorModel = org.areslib.hardware.devices.AresMotorModel.GOBILDA_5203_312_RPM;
      ELEVATOR_CONFIG.externalGearRatio = 1.0;
    }
  }

  public static final class VisionConstants {
    // Maximum weight given to vision updates (0.0 to 1.0)
    public static final double MAX_VISION_TRUST_FACTOR = 0.15;

    // Auto-align default target area
    public static final double ALIGN_TARGET_AREA_PERCENT = 5.0;

    // Confidence calculation heuristics
    public static final double MIN_TARGET_AREA_PERCENT = 0.1;
    public static final double MAX_TRUST_AREA_PERCENT = 1.5;
  }

  public static final class DriveConstants {
    // Teleop User Input Scalars
    public static final double MAX_FWD_SPEED = 3.0;
    public static final double MAX_STR_SPEED = 3.0;
    public static final double MAX_ROT_SPEED = 2.5;

    // Swerve Physical Configuration
    public static final org.firstinspires.ftc.teamcode.subsystems.drive.SwerveConfig SWERVE_CONFIG =
        new org.firstinspires.ftc.teamcode.subsystems.drive.SwerveConfig();

    static {
      SWERVE_CONFIG.trackWidthXMeters = 0.3;
      SWERVE_CONFIG.trackWidthYMeters = 0.3;

      SWERVE_CONFIG.driveKp = 1.0;
      SWERVE_CONFIG.driveKi = 0.0;
      SWERVE_CONFIG.driveKd = 0.0;

      SWERVE_CONFIG.driveKs = 0.1;
      SWERVE_CONFIG.driveKv = 2.5;

      SWERVE_CONFIG.turnKp = 3.0;
      SWERVE_CONFIG.turnKi = 0.0;
      SWERVE_CONFIG.turnKd = 0.0;
      SWERVE_CONFIG.turnKs = 0.2;

      SWERVE_CONFIG.wheelDiameterMM = 104.0; // 104mm standard goBILDA wheel
      SWERVE_CONFIG.driveMotorModel =
          org.areslib.hardware.devices.AresMotorModel.GOBILDA_5203_1620_RPM;
      SWERVE_CONFIG.driveExternalGearRatio = 2.0; // Gearing

      SWERVE_CONFIG.maxModuleSpeedMps = 4.0;
      SWERVE_CONFIG.maxAccelerationMps2 = 4.0; // 4 m/s^2 slew rate limit
    }

    public static final org.firstinspires.ftc.teamcode.subsystems.drive.MecanumConfig
        MECANUM_CONFIG = new org.firstinspires.ftc.teamcode.subsystems.drive.MecanumConfig();

    static {
      MECANUM_CONFIG.maxAccelerationMps2 = 4.0; // 4 m/s^2 slew rate limit
      MECANUM_CONFIG.trackwidthMeters = 0.35; // 35cm
      MECANUM_CONFIG.wheelbaseMeters = 0.35;
      MECANUM_CONFIG.wheelDiameterMM = 96.0;
      MECANUM_CONFIG.driveMotorModel =
          org.areslib.hardware.devices.AresMotorModel.GOBILDA_5203_312_RPM;
      MECANUM_CONFIG.driveExternalGearRatio = 1.0;

      MECANUM_CONFIG.driveKp = 1.0;
      MECANUM_CONFIG.driveKi = 0.0;
      MECANUM_CONFIG.driveKd = 0.0;
      MECANUM_CONFIG.driveKv = 2.5;
      MECANUM_CONFIG.driveKs = 0.1;
    }

    public static final org.firstinspires.ftc.teamcode.subsystems.drive.DifferentialConfig
        DIFFERENTIAL_CONFIG =
            new org.firstinspires.ftc.teamcode.subsystems.drive.DifferentialConfig();

    static {
      DIFFERENTIAL_CONFIG.maxAccelerationMps2 = 4.0; // 4 m/s^2 slew rate limit
      DIFFERENTIAL_CONFIG.trackwidthMeters = 0.40; // 40cm
      DIFFERENTIAL_CONFIG.wheelDiameterMM = 104.0;
      DIFFERENTIAL_CONFIG.driveMotorModel =
          org.areslib.hardware.devices.AresMotorModel.GOBILDA_5203_312_RPM;
      DIFFERENTIAL_CONFIG.driveExternalGearRatio = 1.0;

      DIFFERENTIAL_CONFIG.driveKp = 1.0;
      DIFFERENTIAL_CONFIG.driveKi = 0.0;
      DIFFERENTIAL_CONFIG.driveKd = 0.0;
      DIFFERENTIAL_CONFIG.driveKv = 2.5;
      DIFFERENTIAL_CONFIG.driveKs = 0.1;
    }
  }

  public static final class AlignConstants {
    // Auto Align To Tag Gains
    public static final double ALIGN_P_X = 0.05; // Tunes Strafe to zero out Tx error
    public static final double ALIGN_P_Y = 0.15; // Tunes Forward to zero out Ty area error
  }
}

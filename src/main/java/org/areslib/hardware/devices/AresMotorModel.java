package org.areslib.hardware.devices;

/**
 * Standard predefined FTC motor and encoder models with their corresponding 
 * exact ticks per revolution (CPR).
 */
public enum AresMotorModel {
    /** 312 RPM goBILDA 5203 Series Motor (19.2032:1 ratio) */
    GOBILDA_5203_312_RPM(537.7),
    /** 435 RPM goBILDA 5203 Series Motor (13.7:1 ratio) */
    GOBILDA_5203_435_RPM(384.5),
    /** 1150 RPM goBILDA 5203 Series Motor (5.2:1 ratio) */
    GOBILDA_5203_1150_RPM(145.1),
    /** 1620 RPM goBILDA 5203 Series Motor (3.7:1 ratio) */
    GOBILDA_5203_1620_RPM(103.8),
    /** 223 RPM goBILDA 5203 Series Motor (26.9:1 ratio) */
    GOBILDA_5203_223_RPM(751.8),
    /** 60 RPM goBILDA 5203 Series Motor (104:1 ratio) */
    GOBILDA_5203_60_RPM(3125.0),
    /** standard bare encoder resolution for a 28 PPR encoder (goBILDA bare, REV Core Hex bare) */
    BARE_MOTOR_28_TICKS(28.0),
    /** standard goBILDA Odometry pod encoder (2000 PPR) */
    GOBILDA_ODOMETRY_POD(2000.0),
    /** standard REV Through Bore Absolute/Relative Encoder */
    REV_THROUGH_BORE(8192.0),
    /** REV HD Hex Motor with 20:1 Planetary Gearbox */
    REV_HD_HEX_20_1(560.0),
    /** REV HD Hex Motor with 40:1 Planetary Gearbox */
    REV_HD_HEX_40_1(1120.0),
    /** Custom / Unlisted motor format. Set external custom ticks per rev variable. */
    CUSTOM(0.0);

    private final double ticksPerRev;

    AresMotorModel(double ticksPerRev) {
        this.ticksPerRev = ticksPerRev;
    }

    /**
     * Gets the exact ticks per revolution (CPR) of the motor encoder output shaft.
     * @return ticks per revolution
     */
    public double getTicksPerRev() {
        return ticksPerRev;
    }
}

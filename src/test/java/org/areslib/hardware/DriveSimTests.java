package org.areslib.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class DriveSimTests {

    @Test
    public void testSwerveSimPhysics() {
        SwerveModuleIOSim sim = new SwerveModuleIOSim();
        SwerveModuleIO.SwerveModuleInputs inputs = new SwerveModuleIO.SwerveModuleInputs();

        // set 10 volts drive, 5 volts turn
        sim.setDriveVoltage(10.0);
        sim.setTurnVoltage(5.0);

        // run 1 loop (0.02s)
        sim.updateInputs(inputs);

        // Drive velocity = 10V * 0.4 = 4.0 m/s
        assertEquals(4.0, inputs.driveVelocityMps, 0.001);
        // Drive pos = 4.0 * 0.02 = 0.08 m
        assertEquals(0.08, inputs.drivePositionMeters, 0.001);

        // Turn velocity = 5V * 5.0 = 25.0 rad/s
        assertEquals(25.0, inputs.turnVelocityRadPerSec, 0.001);
        // Turn pos = (25.0 * 0.02) = 0.5 rad (or wrapped, but 0.5 < PI)
        assertEquals(0.5, inputs.turnAbsolutePositionRad, 0.001);
    }

    @Test
    public void testDifferentialSimPhysics() {
        DifferentialDriveIOSim sim = new DifferentialDriveIOSim();
        DifferentialDriveIO.DifferentialDriveInputs inputs = new DifferentialDriveIO.DifferentialDriveInputs();

        sim.setVoltages(12.0, -12.0);
        sim.updateInputs(inputs);

        assertEquals(36.0, inputs.leftVelocityMps, 0.001);
        assertEquals(0.72, inputs.leftPositionMeters, 0.001);
        assertEquals(-36.0, inputs.rightVelocityMps, 0.001);
        assertEquals(-0.72, inputs.rightPositionMeters, 0.001);
    }

    @Test
    public void testMecanumSimPhysics() {
        MecanumDriveIOSim sim = new MecanumDriveIOSim();
        MecanumDriveIO.MecanumDriveInputs inputs = new MecanumDriveIO.MecanumDriveInputs();

        sim.setVoltages(10.0, 5.0, -5.0, -10.0);
        sim.updateInputs(inputs);

        assertEquals(30.0, inputs.frontLeftVelocityMps, 0.001);
        assertEquals(0.6, inputs.frontLeftPositionMeters, 0.001);
        
        assertEquals(15.0, inputs.frontRightVelocityMps, 0.001);
        assertEquals(0.3, inputs.frontRightPositionMeters, 0.001);

        assertEquals(-15.0, inputs.rearLeftVelocityMps, 0.001);
        assertEquals(-0.3, inputs.rearLeftPositionMeters, 0.001);

        assertEquals(-30.0, inputs.rearRightVelocityMps, 0.001);
        assertEquals(-0.6, inputs.rearRightPositionMeters, 0.001);
    }
}

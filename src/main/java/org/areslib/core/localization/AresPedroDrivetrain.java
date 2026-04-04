package org.areslib.core.localization;

import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.math.Vector;
import org.areslib.subsystems.drive.DriveSubsystem;

public class AresPedroDrivetrain extends Drivetrain {

    private final DriveSubsystem driveSubsystem;
    
    // Internal state cache for logging/telemetry
    private double currentForward = 0.0;
    private double currentStrafe = 0.0;
    private double currentTurn = 0.0;

    public AresPedroDrivetrain(DriveSubsystem driveSubsystem) {
        super();
        this.driveSubsystem = driveSubsystem;
    }

    @Override
    public double[] calculateDrive(Vector driveError, Vector headingError, Vector driveVector, double headingVector) {
        // Pedro Pathing calculates error vectors based on inches natively.
        // We will convert these into reasonable inputs for Swerve Drive kinematics.
        // ARESlib DriveSubsystem takes (forwardMetersPerSec, strafeMetersPerSec, turnRadPerSec)
        
        // We can synthesize a final chassis speeds vector from these:
        double forwardInput = driveError.getYComponent() + driveVector.getYComponent();
        double strafeInput = -(driveError.getXComponent() + driveVector.getXComponent());
        double turnInput = headingError.getMagnitude() + headingVector; // Not exact, but placeholder

        // Note: Pedro inputs are in inches. Usually for power control, these get scaled.
        // Converting standard Pedro vectors to meters/sec approximation
        currentForward = forwardInput * 0.0254;
        currentStrafe = strafeInput * 0.0254;
        currentTurn = turnInput;

        driveSubsystem.drive(currentForward, currentStrafe, currentTurn);

        return new double[]{0, 0, 0, 0}; // Return unused power array
    }

    @Override
    public void startTeleopDrive() {}

    @Override
    public void startTeleopDrive(boolean useVoltageCompensation) {}

    @Override
    public void runDrive(double[] powers) {
        // Unused directly, as calculateDrive drives the subsystem asynchronously
    }

    @Override
    public void breakFollowing() {
        driveSubsystem.drive(0, 0, 0);
    }

    @Override
    public double xVelocity() { return 0; }

    @Override
    public double yVelocity() { return 0; }

    @Override
    public void updateConstants() {}

    @Override
    public void setXVelocity(double xV) {}

    @Override
    public void setYVelocity(double yV) {}

    @Override
    public double getVoltage() { return 12.0; }

    @Override
    public String debugString() { return "AresPedroSwerveBridge"; }
}

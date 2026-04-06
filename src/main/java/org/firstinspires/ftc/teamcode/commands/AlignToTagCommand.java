package org.firstinspires.ftc.teamcode.commands;

import org.areslib.command.Command;
import org.areslib.subsystems.drive.SwerveDriveSubsystem;
import org.areslib.subsystems.vision.AresVisionSubsystem;
import static org.firstinspires.ftc.teamcode.Constants.AlignConstants.*;

public class AlignToTagCommand extends Command {
    private final SwerveDriveSubsystem drive;
    private final AresVisionSubsystem vision;
    
    // Proportional Gains (Teams should tune these real-world limits)
    private final double targetArea;

    public AlignToTagCommand(SwerveDriveSubsystem drive, AresVisionSubsystem vision, double targetAreaPercent) {
        this.drive = drive;
        this.vision = vision;
        this.targetArea = targetAreaPercent;
        addRequirements(drive);
    }

    @Override
    public void execute() {
        if (vision.hasTarget()) {
            double tx = vision.getTargetXOffset();
            double ta = vision.getTargetArea();
            
            // X offset correlates to Strafe (Left/Right)
            double strafeCmd = -tx * ALIGN_kPx;
            
            // Area discrepancy correlates to Forward/Back
            double forwardCmd = (targetArea - ta) * ALIGN_kPy;
            
            // Send autonomous velocities to drive base, zero rotation
            drive.drive(forwardCmd, strafeCmd, 0.0);
        } else {
            // Stop if no target found to avoid blind flight
            drive.drive(0.0, 0.0, 0.0);
        }
    }

    @Override
    public void end(boolean interrupted) {
        drive.drive(0.0, 0.0, 0.0);
    }

    @Override
    public boolean isFinished() {
        // Run continuously while button is held
        return false;
    }
}

package org.areslib.command;

import org.areslib.math.controller.PIDController;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * A command that stabilizes the robot's heading when the driver isn't actively rotating.
 * <p>
 * When the driver releases the rotation stick, the command locks the current heading and
 * applies PID correction to prevent drift during strafing. When the driver starts rotating
 * again, the lock is released and raw input passes through.
 * <p>
 * This is drivetrain-agnostic — it works with any drive type by accepting suppliers and consumers.
 *
 * <pre>{@code
 * new HeadingLockCommand(
 *     () -> -gamepad1.right_stick_x,  // rotation input
 *     () -> imu.getYaw(),             // current heading (radians)
 *     omega -> driveOmega = omega,    // omega output
 *     0.5, 0.0, 0.02,                // PID gains
 *     0.05                            // deadband
 * );
 * }</pre>
 */
public class HeadingLockCommand extends Command {

    private final DoubleSupplier m_rotationInput;
    private final DoubleSupplier m_gyroHeading;
    private final DoubleConsumer m_omegaOutput;
    private final PIDController m_headingPID;
    private final double m_deadband;

    private boolean m_isLocked = false;
    private double m_lockedHeading = 0.0;
    private int m_idleCount = 0;
    private static final int IDLE_LOOPS_TO_LOCK = 5; // 100ms at 50Hz

    /**
     * Constructs a HeadingLockCommand.
     *
     * @param rotationInput Supplier for the driver's rotation joystick input (-1 to 1).
     * @param gyroHeading   Supplier for the current gyro heading in radians.
     * @param omegaOutput   Consumer that receives the corrected omega output.
     * @param kP            Heading PID proportional gain.
     * @param kI            Heading PID integral gain.
     * @param kD            Heading PID derivative gain.
     * @param deadband      The rotation input deadband below which heading lock engages.
     */
    public HeadingLockCommand(
            DoubleSupplier rotationInput,
            DoubleSupplier gyroHeading,
            DoubleConsumer omegaOutput,
            double kP, double kI, double kD,
            double deadband) {
        m_rotationInput = rotationInput;
        m_gyroHeading = gyroHeading;
        m_omegaOutput = omegaOutput;
        m_headingPID = new PIDController(kP, kI, kD);
        m_headingPID.enableContinuousInput(-Math.PI, Math.PI);
        m_deadband = deadband;
    }

    @Override
    public void initialize() {
        m_isLocked = false;
        m_idleCount = 0;
        m_headingPID.reset();
    }

    @Override
    public void execute() {
        double rotInput = m_rotationInput.getAsDouble();
        double currentHeading = m_gyroHeading.getAsDouble();

        if (Math.abs(rotInput) > m_deadband) {
            // Driver is actively rotating — pass through raw input
            m_isLocked = false;
            m_idleCount = 0;
            m_headingPID.reset();
            m_omegaOutput.accept(rotInput);
        } else {
            m_idleCount++;

            if (!m_isLocked && m_idleCount >= IDLE_LOOPS_TO_LOCK) {
                // Lock the heading after being idle for enough loops
                m_isLocked = true;
                m_lockedHeading = currentHeading;
            }

            if (m_isLocked) {
                // Apply PID correction to hold locked heading
                double correction = m_headingPID.calculate(currentHeading, m_lockedHeading);
                m_omegaOutput.accept(correction);
            } else {
                m_omegaOutput.accept(0.0);
            }
        }
    }

    /**
     * Returns whether the heading lock is currently active.
     */
    public boolean isLocked() {
        return m_isLocked;
    }
}

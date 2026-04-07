package org.areslib.command.sysid;

import org.areslib.command.Command;
import org.areslib.core.AresRobot;
import org.areslib.telemetry.AresAutoLogger;

/**
 * The executing state machine that applies exact voltage over time and logs
 * the mechanism's instantaneous state in the official WPILib `SysId` format.
 */
public class SysIdCommand extends Command {

    private final SysIdRoutine.Config m_config;
    private final SysIdRoutine.Mechanism m_mechanism;
    private final boolean m_isQuasistatic;
    private final SysIdRoutine.Direction m_direction;
    private final String m_stateName;
    
    private double m_accumulator = 0.0;
    /**
     * Constructs a new SysId test command.
     * 
     * @param config        The SysId configuration constraints.
     * @param mechanism     The mechanism bindings allowing I/O access.
     * @param isQuasistatic True if the test is a slow voltage ramp (quasistatic). False for an instant step (dynamic).
     * @param direction     The direction of the test.
     */
    public SysIdCommand(SysIdRoutine.Config config, SysIdRoutine.Mechanism mechanism, boolean isQuasistatic, SysIdRoutine.Direction direction) {
        m_config = config;
        m_mechanism = mechanism;
        m_isQuasistatic = isQuasistatic;
        m_direction = direction;
        
        m_stateName = (isQuasistatic ? "quasistatic" : "dynamic") + "-" + (direction == SysIdRoutine.Direction.FORWARD ? "forward" : "reverse");
        
        addRequirements(mechanism.requirements);
    }

    @Override
    public void initialize() {
        m_accumulator = 0.0;
        AresAutoLogger.recordOutput("SysId/State", m_stateName);
    }

    @Override
    public void execute() {
        m_accumulator += AresRobot.LOOP_PERIOD_SECS;
        
        double volts;
        if (m_isQuasistatic) {
            volts = m_accumulator * m_config.rampRateVoltsPerSec;
        } else {
            volts = m_config.stepVoltageVolts;
        }
        volts *= m_direction.multiplier;
        
        m_mechanism.voltageInput.accept(volts);
        
        // Push standardized keys to telemetry
        AresAutoLogger.recordOutput("SysId/Voltage", volts);
        AresAutoLogger.recordOutput("SysId/Position", m_mechanism.positionOutput.get());
        AresAutoLogger.recordOutput("SysId/Velocity", m_mechanism.velocityOutput.get());
    }

    @Override
    public boolean isFinished() {
        return m_accumulator >= m_config.timeoutSeconds;
    }

    @Override
    public void end(boolean interrupted) {
        m_mechanism.voltageInput.accept(0.0);
        AresAutoLogger.recordOutput("SysId/State", "none");
        AresAutoLogger.recordOutput("SysId/Voltage", 0.0);
    }
}

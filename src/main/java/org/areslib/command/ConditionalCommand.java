package org.areslib.command;

import java.util.function.BooleanSupplier;

/**
 * A command that runs one of two commands depending on the evaluation of a given condition when
 * this command is initialized.
 */
public class ConditionalCommand extends Command {
    private final Command m_onTrue;
    private final Command m_onFalse;
    private final BooleanSupplier m_condition;
    private Command m_selectedCommand;

    /**
     * Creates a new ConditionalCommand.
     *
     * @param onTrue    the command to run if the condition evaluates to true
     * @param onFalse   the command to run if the condition evaluates to false
     * @param condition the condition to evaluate
     */
    public ConditionalCommand(Command onTrue, Command onFalse, BooleanSupplier condition) {
        m_onTrue = onTrue;
        m_onFalse = onFalse;
        m_condition = condition;

        m_requirements.addAll(m_onTrue.getRequirements());
        m_requirements.addAll(m_onFalse.getRequirements());
    }

    @Override
    public void initialize() {
        if (m_condition.getAsBoolean()) {
            m_selectedCommand = m_onTrue;
        } else {
            m_selectedCommand = m_onFalse;
        }
        m_selectedCommand.initialize();
    }

    @Override
    public void execute() {
        m_selectedCommand.execute();
    }

    @Override
    public void end(boolean interrupted) {
        m_selectedCommand.end(interrupted);
    }

    @Override
    public boolean isFinished() {
        return m_selectedCommand.isFinished();
    }
}

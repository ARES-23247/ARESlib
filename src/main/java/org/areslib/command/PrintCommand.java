package org.areslib.command;

/**
 * A command that prints a string to standard output when initialized.
 */
public class PrintCommand extends InstantCommand {
    /**
     * Creates a new PrintCommand.
     *
     * @param message the message to print
     */
    public PrintCommand(String message) {
        super(() -> System.out.println(message));
    }

}

package org.areslib.faults;

/**
 * Represents a hardware alert or state warning that can be displayed to the driver station.
 */
public class AresAlert {
    public enum AlertType {
        /**
         * Critical errors that stop subsystems from functioning, such as hardware disconnects.
         */
        ERROR,
        
        /**
         * Warning conditions like brownouts or suboptimal loop times.
         */
        WARNING,
        
        /**
         * General information.
         */
        INFO
    }

    private String text;
    private final AlertType type;
    private boolean active;

    /**
     * Creates a new Alert and automatically registers it with the AresFaultManager.
     * @param text The text to display.
     * @param type The severity type.
     */
    public AresAlert(String text, AlertType type) {
        this.text = text;
        this.type = type;
        this.active = false;
        AresFaultManager.registerAlert(this);
    }

    /**
     * Sets whether this alert is currently active.
     */
    public void set(boolean active) {
        this.active = active;
    }

    /**
     * Sets the text for this alert dynamically.
     */
    public void setText(String text) {
        this.text = text;
    }

    public boolean isActive() {
        return active;
    }

    public String getText() {
        return text;
    }

    public AlertType getType() {
        return type;
    }
}

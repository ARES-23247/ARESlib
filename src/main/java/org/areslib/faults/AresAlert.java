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
     * <p>
     * If this alert was previously cleared from the {@link AresFaultManager}
     * (e.g., during an OpMode transition reset), calling this method will
     * automatically re-register it, ensuring static alerts survive lifecycle resets.
     */
    public void set(boolean active) {
        this.active = active;
        // Re-register if we were dropped by a reset() cycle.
        // registerAlert() is idempotent — it no-ops if already present.
        AresFaultManager.registerAlert(this);
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

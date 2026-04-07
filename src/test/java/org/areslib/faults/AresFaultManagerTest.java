package org.areslib.faults;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AresFaultManagerTest {

    @BeforeEach
    public void setup() {
        AresFaultManager.reset();
    }

    @AfterEach
    public void teardown() {
        AresFaultManager.reset();
    }

    @Test
    public void testAlertRegistrationAndState() {
        AresAlert errorAlert = new AresAlert("Critical Error", AresAlert.AlertType.ERROR);
        
        // Initially false
        assertFalse(errorAlert.isActive());
        
        // Set it active
        errorAlert.set(true);
        assertTrue(errorAlert.isActive());
        assertEquals(AresAlert.AlertType.ERROR, errorAlert.getType());
        assertEquals("Critical Error", errorAlert.getText());
        
        // Calling update processes it
        AresFaultManager.update();
        
        // Verify we can safely set it false
        errorAlert.set(false);
        assertFalse(errorAlert.isActive());
    }

    @Test
    public void testAlertReRegistrationAfterLifecycleReset() {
        AresAlert warnAlert = new AresAlert("Low Battery", AresAlert.AlertType.WARNING);
        warnAlert.set(true);
        
        // Reset wipes the active manager state
        AresFaultManager.reset();
        
        // In the next OpMode, setting the static alert back to active should re-register it
        warnAlert.set(true);
        assertTrue(warnAlert.isActive());
        
        // It shouldn't crash on update
        AresFaultManager.update();
    }
}

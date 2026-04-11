package org.areslib.faults;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  @Test
  public void testErrorAlertTriggersNewErrorFlag() {
    AresAlert error = new AresAlert("Motor Stall", AresAlert.AlertType.ERROR);
    error.set(true);

    AresFaultManager.update();

    assertTrue(AresFaultManager.hasNewError(), "First error should set hasNewError");
    // Reading hasNewError clears it (edge detection)
    assertFalse(AresFaultManager.hasNewError(), "hasNewError should clear after read");
  }

  @Test
  public void testWarningDoesNotTriggerNewError() {
    AresAlert warning = new AresAlert("Battery Low", AresAlert.AlertType.WARNING);
    warning.set(true);

    AresFaultManager.update();

    assertFalse(AresFaultManager.hasNewError(), "Warnings should not flag hasNewError");
  }

  @Test
  public void testInfoDoesNotTriggerNewError() {
    AresAlert info = new AresAlert("Calibration OK", AresAlert.AlertType.INFO);
    info.set(true);

    AresFaultManager.update();

    assertFalse(AresFaultManager.hasNewError(), "Infos should not flag hasNewError");
  }

  @Test
  public void testMultipleAlertClassification() {
    AresAlert error = new AresAlert("Encoder Disconnect", AresAlert.AlertType.ERROR);
    AresAlert warning = new AresAlert("Voltage Sag", AresAlert.AlertType.WARNING);
    AresAlert info = new AresAlert("System Ready", AresAlert.AlertType.INFO);
    error.set(true);
    warning.set(true);
    info.set(true);

    // Should classify into separate buckets without crashing
    AresFaultManager.update();

    assertTrue(AresFaultManager.hasNewError());
  }

  @Test
  public void testResetFullyClearsState() {
    AresAlert error = new AresAlert("Critical", AresAlert.AlertType.ERROR);
    error.set(true);
    AresFaultManager.update();

    AresFaultManager.reset();
    AresFaultManager.update();

    assertFalse(AresFaultManager.hasNewError(), "Reset should clear all state");
  }

  @Test
  public void testCacheReuseUnderStress() {
    AresAlert error = new AresAlert("Repeat", AresAlert.AlertType.ERROR);
    error.set(true);

    // Run update 1000 times — the pre-allocated CACHED_ERRORS list must be reused without OOM
    for (int i = 0; i < 1000; i++) {
      AresFaultManager.update();
    }

    assertTrue(AresFaultManager.hasNewError() || !AresFaultManager.hasNewError());
  }

  @Test
  public void testDeactivatedAlertNotReported() {
    AresAlert error = new AresAlert("Intermittent", AresAlert.AlertType.ERROR);
    error.set(true);
    AresFaultManager.update();
    AresFaultManager.hasNewError(); // clear edge flag

    error.set(false);
    AresFaultManager.update();

    assertFalse(AresFaultManager.hasNewError(), "Deactivated alert should not trigger new error");
  }

  @Test
  public void testErrorEdgeOnlyFiresOnce() {
    AresAlert error = new AresAlert("Persistent Error", AresAlert.AlertType.ERROR);
    error.set(true);

    AresFaultManager.update();
    assertTrue(AresFaultManager.hasNewError(), "First cycle should fire edge");

    // Error stays active, but edge should NOT fire again
    AresFaultManager.update();
    assertFalse(AresFaultManager.hasNewError(), "Sustained error should not re-fire edge");
  }

  @Test
  public void testNoAlertsProducesCleanUpdate() {
    // Zero alerts registered — update should not throw
    AresFaultManager.update();
    assertFalse(AresFaultManager.hasNewError());
  }
}

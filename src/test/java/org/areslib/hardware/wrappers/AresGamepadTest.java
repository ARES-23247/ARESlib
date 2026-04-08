package org.areslib.hardware.wrappers;

import static org.junit.jupiter.api.Assertions.*;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.areslib.command.button.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AresGamepadTest {

  @Mock private Gamepad mockGamepad;

  private AresGamepad aresGamepad;

  @BeforeEach
  public void setup() {
    aresGamepad = new AresGamepad(mockGamepad);
  }

  @Test
  public void testAxisInversions() {
    // Test standard behavior where raw physical down (-1.0 on most controllers)
    // maps to WTILib generic 'Up is Positive'
    mockGamepad.left_stick_y = -1.0f; // Pushed fully UP on FTC Gamepad
    mockGamepad.right_stick_y = 0.5f; // Pushed partially DOWN

    assertEquals(1.0, aresGamepad.getLeftY(), 1e-6, "Left Y should be inverted to positive");
    assertEquals(-0.5, aresGamepad.getRightY(), 1e-6, "Right Y should be inverted to negative");
  }

  @Test
  public void testLazyCacheTriggers() {
    // Assert initial trigger bindings return same cached instance
    Trigger a1 = aresGamepad.a();
    Trigger a2 = aresGamepad.a();
    assertSame(a1, a2, "Triggers should be lazily cached and identical instances");

    // Verify active state delegation
    mockGamepad.a = true;
    assertTrue(a1.getAsBoolean(), "Trigger should evaluate correctly against mocked hardware");

    mockGamepad.a = false;
    assertFalse(a1.getAsBoolean(), "Trigger should reflect deactivated physical state");
  }
}

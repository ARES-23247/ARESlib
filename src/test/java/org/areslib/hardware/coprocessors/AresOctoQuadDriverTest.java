package org.areslib.hardware.coprocessors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class AresOctoQuadDriverTest {

  @Test
  void testInterfaceContract() {
    AresOctoQuadDriver mockDriver = mock(AresOctoQuadDriver.class);

    // Test position read
    when(mockDriver.readPosition(1)).thenReturn(100.0);
    assertEquals(100.0, mockDriver.readPosition(1));

    // Test pulse width read
    when(mockDriver.readPulseWidth(2)).thenReturn(5.5);
    assertEquals(5.5, mockDriver.readPulseWidth(2));

    // Verify method invocations
    mockDriver.clearReadCache();
    verify(mockDriver, times(1)).clearReadCache();

    mockDriver.setChannelBankConfig(0, OctoMode.ENCODER);
    verify(mockDriver, times(1)).setChannelBankConfig(0, OctoMode.ENCODER);
  }
}

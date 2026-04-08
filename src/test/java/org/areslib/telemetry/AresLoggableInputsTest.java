package org.areslib.telemetry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AresLoggableInputsTest {

  static class DummyInputs implements AresLoggableInputs {
    public double testValue = 3.14;
  }

  @Test
  void testMarkerInterface() {
    DummyInputs inputs = new DummyInputs();

    // Ensure the interface properly acts as an instanceof tag
    assertTrue(inputs instanceof AresLoggableInputs);

    // In actual implementation, AresAutoLogger uses reflection to walk through
    // public fields of classes implementing this interface. We verify instantiation here.
    org.junit.jupiter.api.Assertions.assertEquals(3.14, inputs.testValue);
  }
}

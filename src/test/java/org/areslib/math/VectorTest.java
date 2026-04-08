package org.areslib.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VectorTest {

  @Test
  void testVectorStorage() {
    Vector<?> vec = new Vector<>(1.0, 2.0, 3.0);
    assertEquals(1.0, vec.get(0));
    assertEquals(2.0, vec.get(1));
    assertEquals(3.0, vec.get(2));
  }
}

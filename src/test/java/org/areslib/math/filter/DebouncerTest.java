package org.areslib.math.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Debouncer}. */
class DebouncerTest {

  @Test
  @DisplayName("Rising debounce requires sustained true signal")
  void risingDebounce() {
    Debouncer debouncer = new Debouncer(0.1, Debouncer.DebounceType.kRising);
    // Baseline starts false. Passing true accumulates time.
    assertFalse(debouncer.calculate(true, 0.02)); // 0.02s elapsed
    assertFalse(debouncer.calculate(true, 0.02)); // 0.04s
    assertFalse(debouncer.calculate(true, 0.02)); // 0.06s
    assertFalse(debouncer.calculate(true, 0.02)); // 0.08s
    assertTrue(debouncer.calculate(true, 0.02)); // 0.10s >= 0.1 → baseline flips to true
  }

  @Test
  @DisplayName("Rising debounce resets when signal goes low")
  void risingDebounceResets() {
    Debouncer debouncer = new Debouncer(0.1, Debouncer.DebounceType.kRising);
    debouncer.calculate(true, 0.05); // 50ms accumulated
    debouncer.calculate(false, 0.02); // signal = baseline(false) → timer resets
    // Need full time again
    assertFalse(debouncer.calculate(true, 0.05)); // 50ms
    assertFalse(debouncer.calculate(true, 0.04)); // 90ms
    assertTrue(debouncer.calculate(true, 0.02)); // 110ms >= 100ms
  }

  @Test
  @DisplayName("kBoth rising transition")
  void bothTypeRising() {
    Debouncer debouncer = new Debouncer(0.05, Debouncer.DebounceType.kBoth);
    // Rising: baseline=false → true
    assertFalse(debouncer.calculate(true, 0.03)); // 30ms
    assertFalse(debouncer.calculate(true, 0.01)); // 40ms
    assertTrue(debouncer.calculate(true, 0.02)); // 60ms >= 50ms → baseline=true
  }

  @Test
  @DisplayName("Stable baseline returns consistently")
  void stableBaseline() {
    Debouncer debouncer = new Debouncer(1.0, Debouncer.DebounceType.kRising);
    // Matching baseline input returns baseline
    assertFalse(debouncer.calculate(false, 0.02));
    assertFalse(debouncer.calculate(false, 0.02));
    assertFalse(debouncer.calculate(false, 0.02));
  }

  @Test
  @DisplayName("After baseline flips, matching input returns new baseline")
  void afterFlip() {
    Debouncer debouncer = new Debouncer(0.05, Debouncer.DebounceType.kRising);
    // Flip baseline to true
    debouncer.calculate(true, 0.03);
    debouncer.calculate(true, 0.03); // baseline is now true
    // Now input=true matches baseline, should return true
    assertTrue(debouncer.calculate(true, 0.02));
  }
}

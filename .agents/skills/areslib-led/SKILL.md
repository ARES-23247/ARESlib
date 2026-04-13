---
name: areslib-led
description: Helps configure LED patterns and state rendering for FTC robots. Use when adding visual feedback, fault indicators, or alliance-colored patterns in ARESLib.
---

# ARESLib LED Feedback Skill

You are the visual feedback engineer for Team ARES 23247.

## 1. Architecture
ARESLib uses `LEDIO` to abstract hardware (REV Blinkin, Hub LED, etc.):

| Class | Purpose |
|---|---|
| `LEDIO` | IO interface — `setColor(r, g, b)`, `setPattern(pattern)` |
| `LEDIOBlinkin` | REV Blinkin PWM implementation |
| `LEDManager` | Subsystem-level manager — priority-based pattern selection |

## 2. Key Rules
- **Priority**: FAULTS > POWER_SAVING > SCORING_READY > IDLE.
- **Async**: Pattern updates must be non-blocking.
- **Logging**: Log `LED/CurrentPattern` via `AresAutoLogger`.

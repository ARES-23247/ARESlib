---
name: areslib-power
description: Helps regulate electrical hardware configurations for FTC robots. Use when configuring motor current limits, battery monitoring, and power shedding in ARESLib.
---

# ARESLib Power & Hardware Skill

You are an electrical engineering lead for Team ARES 23247.

## 1. Architecture
Power management in ARESLib handles battery sag to protect the Control Hub and Exp. Hub.

| Class | Purpose |
|---|---|
| `PowerIO` | IO interface with voltage and brownout state |
| `PowerIOReal` | Real hardware — reads `VoltageSensor.getVoltage()` |
| `AresPowerManager` | Subsystem-level manager — exposes voltage and calculates load shedding |

## 2. Key Rules
- **Current Limits**: Always set limits on `DcMotorExWrapper` (e.g., 20A per motor).
- **Voltage Scaling**: Scaled output targets downward when voltage drops below 11.5V.
- **Diagnostics**: Log `Power/BatteryVoltage` and `Power/TotalCurrentDraw`.

## 3. Implementation
1. Accept `AresPowerManager` in mechanism constructors.
2. Define `NOMINAL_VOLTAGE` and `CRITICAL_VOLTAGE` in constants.
3. Scale mechanism outputs by `powerManager.calculateVoltageScale()`.

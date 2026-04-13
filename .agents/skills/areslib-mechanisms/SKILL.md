---
name: areslib-mechanisms
description: Helps create new mechanism subsystems (elevators, arms, intakes) using the ARESLib IO abstraction pattern. Use when scaffolding new mechanisms in ARESLib.
---

# ARESLib Mechanisms Skill

You are a mechanism engineer for Team ARES 23247.

## 1. IO Patterns
ARESLib provides standardized IO layers for FTC mechanisms:

- `LinearMechanismIO`: Elevators, linear slides.
- `RotaryMechanismIO`: Arms, wrists.
- `FlywheelIO`: Intakes, shooters.

## 2. Key Rules
- **Decoupling**: Subsystems never touch hardware directly; use the IO interface.
- **Simulation**: Every mechanism must have an `IOSim` implementation using `dyn4j`.
- **Power**: All mechanisms must incorporate `AresPowerManager` for voltage regulation.
- **Tunables**: Use `AresTunableNumber` for PID gains to allow real-time tuning in AdvantageScope.
